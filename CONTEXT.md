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

**Schedule**:
A planned appointment or event that a **User** wants OnTime to help them prepare for.
_Avoid_: Event, calendar item

**Preparation**:
A step a **User** needs to complete before a **Schedule**.
_Avoid_: Task, checklist item

**Alarm Window**:
A bounded time range where OnTime identifies **Schedules** that need alarm coverage.
_Avoid_: Notification window, schedule window

## Relationships

- A **User** has at most one **Active Session**.
- A **User Session** belongs to exactly one **User**.
- A **User** owns zero or more **Schedules**.
- A **Schedule** uses one or more **Preparations** to determine when the **User** should start getting ready.
- An **Alarm Window** contains zero or more **Schedules** for a single **User**.

## Example Dialogue

> **Dev:** "If a **User** signs in on a second phone, do both phones keep an **Active Session**?"
> **Domain expert:** "No. The second sign-in becomes the **Active Session**, and the previous **User Session** is no longer allowed to use protected APIs."

> **Dev:** "When the app asks for an **Alarm Window**, should it include the **Preparations** for each **Schedule**?"
> **Domain expert:** "Yes. The app needs those **Preparations** to calculate when the **User** should start getting ready."

## Flagged Ambiguities

- "device login" was used to mean both a physical device and an authenticated **User Session**. Resolved: the login limit applies to **User Sessions**, not to registered alarm devices.
