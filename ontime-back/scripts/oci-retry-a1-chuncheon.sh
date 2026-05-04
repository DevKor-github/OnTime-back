#!/usr/bin/env bash
set -euo pipefail

# Repeatedly retry the OCI Always Free A1 Flex VM launch without re-entering
# the browser form. Safe to rerun: network resources are reused by display name.

OCI_REGION="${OCI_REGION:-ap-chuncheon-1}"
OCI_PROFILE="${OCI_PROFILE:-DEFAULT}"

INSTANCE_NAME="${INSTANCE_NAME:-ontime}"
COMPARTMENT_ID="${COMPARTMENT_ID:-}"
AVAILABILITY_DOMAIN="${AVAILABILITY_DOMAIN:-Cxab:AP-CHUNCHEON-1-AD-1}"

SHAPE="${SHAPE:-VM.Standard.A1.Flex}"
OCPUS="${OCPUS:-1}"
MEMORY_IN_GBS="${MEMORY_IN_GBS:-6}"

VCN_NAME="${VCN_NAME:-ontime-vcn}"
VCN_CIDR="${VCN_CIDR:-10.0.0.0/16}"
VCN_DNS_LABEL="${VCN_DNS_LABEL:-ontimevcn}"
SUBNET_NAME="${SUBNET_NAME:-ontime-public-subnet}"
SUBNET_CIDR="${SUBNET_CIDR:-10.0.0.0/24}"
SUBNET_DNS_LABEL="${SUBNET_DNS_LABEL:-ontimesubnet}"
INTERNET_GATEWAY_NAME="${INTERNET_GATEWAY_NAME:-ontime-igw}"

SSH_PUBLIC_KEY_FILE="${SSH_PUBLIC_KEY_FILE:-$HOME/.ssh/id_ed25519.pub}"
SSH_SOURCE_CIDR="${SSH_SOURCE_CIDR:-0.0.0.0/0}"
API_SOURCE_CIDR="${API_SOURCE_CIDR:-0.0.0.0/0}"
RETRY_ATTEMPTS="${RETRY_ATTEMPTS:-1}"
RETRY_SLEEP_SECONDS="${RETRY_SLEEP_SECONDS:-900}"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

oci_json() {
  oci --region "$OCI_REGION" --profile "$OCI_PROFILE" "$@" --output json
}

oci_raw() {
  oci --region "$OCI_REGION" --profile "$OCI_PROFILE" "$@" --raw-output
}

first_id_by_display_name() {
  jq -r --arg name "$1" '.data[] | select(."display-name" == $name) | .id' | head -n 1
}

create_or_get_vcn() {
  local vcn_id
  vcn_id="$(
    oci_json network vcn list \
      --compartment-id "$COMPARTMENT_ID" \
      --lifecycle-state AVAILABLE |
      first_id_by_display_name "$VCN_NAME"
  )"

  if [[ -n "$vcn_id" ]]; then
    echo "$vcn_id"
    return
  fi

  oci_raw network vcn create \
    --compartment-id "$COMPARTMENT_ID" \
    --cidr-block "$VCN_CIDR" \
    --display-name "$VCN_NAME" \
    --dns-label "$VCN_DNS_LABEL" \
    --wait-for-state AVAILABLE \
    --query 'data.id'
}

create_or_get_internet_gateway() {
  local vcn_id="$1"
  local igw_id

  igw_id="$(
    oci_json network internet-gateway list \
      --compartment-id "$COMPARTMENT_ID" \
      --vcn-id "$vcn_id" \
      --lifecycle-state AVAILABLE |
      first_id_by_display_name "$INTERNET_GATEWAY_NAME"
  )"

  if [[ -n "$igw_id" ]]; then
    echo "$igw_id"
    return
  fi

  oci_raw network internet-gateway create \
    --compartment-id "$COMPARTMENT_ID" \
    --vcn-id "$vcn_id" \
    --is-enabled true \
    --display-name "$INTERNET_GATEWAY_NAME" \
    --wait-for-state AVAILABLE \
    --query 'data.id'
}

configure_route_table() {
  local route_table_id="$1"
  local igw_id="$2"
  local route_rules_file="$tmp_dir/route-rules.json"

  jq -n --arg igw_id "$igw_id" \
    '[{"cidrBlock":"0.0.0.0/0","networkEntityId":$igw_id}]' \
    > "$route_rules_file"

  oci_json network route-table update \
    --rt-id "$route_table_id" \
    --route-rules "file://$route_rules_file" \
    --force >/dev/null
}

configure_security_list() {
  local security_list_id="$1"
  local ingress_rules_file="$tmp_dir/ingress-rules.json"
  local egress_rules_file="$tmp_dir/egress-rules.json"

  jq -n \
    --arg ssh_source "$SSH_SOURCE_CIDR" \
    --arg api_source "$API_SOURCE_CIDR" \
    '[
      {
        "protocol":"6",
        "source":$ssh_source,
        "sourceType":"CIDR_BLOCK",
        "tcpOptions":{"destinationPortRange":{"min":22,"max":22}},
        "isStateless":false
      },
      {
        "protocol":"6",
        "source":$api_source,
        "sourceType":"CIDR_BLOCK",
        "tcpOptions":{"destinationPortRange":{"min":8080,"max":8080}},
        "isStateless":false
      }
    ]' > "$ingress_rules_file"

  jq -n '[
    {
      "protocol":"all",
      "destination":"0.0.0.0/0",
      "destinationType":"CIDR_BLOCK",
      "isStateless":false
    }
  ]' > "$egress_rules_file"

  oci_json network security-list update \
    --security-list-id "$security_list_id" \
    --ingress-security-rules "file://$ingress_rules_file" \
    --egress-security-rules "file://$egress_rules_file" \
    --force >/dev/null
}

create_or_get_subnet() {
  local vcn_id="$1"
  local route_table_id="$2"
  local security_list_id="$3"
  local subnet_id
  local security_list_ids_file="$tmp_dir/security-list-ids.json"

  subnet_id="$(
    oci_json network subnet list \
      --compartment-id "$COMPARTMENT_ID" \
      --vcn-id "$vcn_id" \
      --lifecycle-state AVAILABLE |
      first_id_by_display_name "$SUBNET_NAME"
  )"

  if [[ -n "$subnet_id" ]]; then
    echo "$subnet_id"
    return
  fi

  jq -n --arg security_list_id "$security_list_id" '[$security_list_id]' \
    > "$security_list_ids_file"

  oci_raw network subnet create \
    --compartment-id "$COMPARTMENT_ID" \
    --vcn-id "$vcn_id" \
    --cidr-block "$SUBNET_CIDR" \
    --display-name "$SUBNET_NAME" \
    --dns-label "$SUBNET_DNS_LABEL" \
    --route-table-id "$route_table_id" \
    --security-list-ids "file://$security_list_ids_file" \
    --prohibit-public-ip-on-vnic false \
    --wait-for-state AVAILABLE \
    --query 'data.id'
}

latest_oracle_linux_9_image_id() {
  oci_raw compute image list \
    --compartment-id "$COMPARTMENT_ID" \
    --operating-system "Oracle Linux" \
    --operating-system-version "9" \
    --shape "$SHAPE" \
    --lifecycle-state AVAILABLE \
    --sort-by TIMECREATED \
    --sort-order DESC \
    --all \
    --query 'data[0].id'
}

active_instance_id() {
  oci_json compute instance list \
    --compartment-id "$COMPARTMENT_ID" \
    --display-name "$INSTANCE_NAME" \
    --all |
    jq -r '.data[] |
      select(."lifecycle-state" != "TERMINATED") |
      .id' |
    head -n 1
}

public_ip_for_instance() {
  local instance_id="$1"
  local vnic_id

  vnic_id="$(
    oci_raw compute vnic-attachment list \
      --compartment-id "$COMPARTMENT_ID" \
      --instance-id "$instance_id" \
      --query 'data[0]."vnic-id"'
  )"

  if [[ -z "$vnic_id" || "$vnic_id" == "null" ]]; then
    echo ""
    return
  fi

  oci_raw network vnic get \
    --vnic-id "$vnic_id" \
    --query 'data."public-ip"'
}

launch_instance() {
  local image_id="$1"
  local subnet_id="$2"
  local attempt=1
  local err_file
  local instance_id
  local status

  while (( attempt <= RETRY_ATTEMPTS )); do
    err_file="$tmp_dir/launch-${attempt}.err"
    echo "Launch attempt ${attempt}/${RETRY_ATTEMPTS}..." >&2

    set +e
    instance_id="$(
      oci_raw compute instance launch \
        --availability-domain "$AVAILABILITY_DOMAIN" \
        --compartment-id "$COMPARTMENT_ID" \
        --display-name "$INSTANCE_NAME" \
        --image-id "$image_id" \
        --shape "$SHAPE" \
        --shape-config "{\"ocpus\":${OCPUS},\"memoryInGBs\":${MEMORY_IN_GBS}}" \
        --subnet-id "$subnet_id" \
        --assign-public-ip true \
        --assign-private-dns-record true \
        --ssh-authorized-keys-file "$SSH_PUBLIC_KEY_FILE" \
        --wait-for-state RUNNING \
        --query 'data.id' \
        2>"$err_file"
    )"
    status=$?
    set -e

    if (( status == 0 )); then
      echo "$instance_id"
      return 0
    fi

    if grep -qi "out of capacity" "$err_file" && (( attempt < RETRY_ATTEMPTS )); then
      echo "OCI is still out of A1 capacity. Sleeping ${RETRY_SLEEP_SECONDS}s before retry..." >&2
      sleep "$RETRY_SLEEP_SECONDS"
      attempt=$((attempt + 1))
      continue
    fi

    cat "$err_file" >&2
    return "$status"
  done
}

main() {
  require_command oci
  require_command jq

  if [[ -z "$COMPARTMENT_ID" ]]; then
    echo "Set COMPARTMENT_ID to your OCI compartment/root tenancy OCID first." >&2
    exit 1
  fi

  if [[ ! -f "$SSH_PUBLIC_KEY_FILE" ]]; then
    echo "SSH public key not found: $SSH_PUBLIC_KEY_FILE" >&2
    exit 1
  fi

  echo "Using region: $OCI_REGION"
  echo "Using availability domain: $AVAILABILITY_DOMAIN"
  echo "Using shape: $SHAPE (${OCPUS} OCPU, ${MEMORY_IN_GBS} GB RAM)"

  local existing_instance_id
  existing_instance_id="$(active_instance_id)"
  if [[ -n "$existing_instance_id" ]]; then
    echo "Found existing non-terminated instance: $existing_instance_id"
    echo "Public IP: $(public_ip_for_instance "$existing_instance_id")"
    return
  fi

  local vcn_id igw_id route_table_id security_list_id subnet_id image_id instance_id public_ip

  vcn_id="$(create_or_get_vcn)"
  igw_id="$(create_or_get_internet_gateway "$vcn_id")"

  route_table_id="$(
    oci_raw network vcn get \
      --vcn-id "$vcn_id" \
      --query 'data."default-route-table-id"'
  )"
  security_list_id="$(
    oci_raw network vcn get \
      --vcn-id "$vcn_id" \
      --query 'data."default-security-list-id"'
  )"

  configure_route_table "$route_table_id" "$igw_id"
  configure_security_list "$security_list_id"
  subnet_id="$(create_or_get_subnet "$vcn_id" "$route_table_id" "$security_list_id")"
  image_id="$(latest_oracle_linux_9_image_id)"

  if [[ -z "$image_id" || "$image_id" == "null" ]]; then
    echo "Could not find an Oracle Linux 9 image for $SHAPE in $OCI_REGION." >&2
    exit 1
  fi

  echo "Launching $INSTANCE_NAME. If OCI is still out of A1 capacity, this is the step that will fail."
  instance_id="$(launch_instance "$image_id" "$subnet_id")"

  public_ip="$(public_ip_for_instance "$instance_id")"

  echo "Created instance: $instance_id"
  echo "Public IP: $public_ip"
  echo "SSH user for Oracle Linux images is usually: opc"
  echo "SSH command: ssh opc@$public_ip"
}

main "$@"
