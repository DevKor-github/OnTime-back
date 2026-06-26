# OnTime Backend

OnTime Backend handles user identity, schedules, preparation data, alarms, and account preferences for the OnTime application.

## Language

**User**:
A person who owns OnTime account data and authenticates to use the app.
_Avoid_: Account, member

**User Session**:
An authenticated app session for a **User**.
_Avoid_: Device login, token slot

**Active Session**:
The single **User Session** currently allowed to access protected OnTime APIs.
_Avoid_: Current token, latest device

## Relationships

- A **User** has at most one **Active Session**.
- A **User Session** belongs to exactly one **User**.

## Example Dialogue

> **Dev:** "If a **User** signs in on a second phone, do both phones keep an **Active Session**?"
> **Domain expert:** "No. The second sign-in becomes the **Active Session**, and the previous **User Session** is no longer allowed to use protected APIs."

## Flagged Ambiguities

- "device login" was used to mean both a physical device and an authenticated **User Session**. Resolved: the login limit applies to **User Sessions**, not to registered alarm devices.
