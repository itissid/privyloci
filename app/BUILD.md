
# How am I proceeding about this build:

0. First the usecases I can think of, with some examples
1. Next, I will list out the core Requirements for the application I am building.
2. I will build swimlane diagrams for the core requirements.
3. A roadmap with the datamodel and some of the key code components to build.
4. Once and only once built I want to properly message out and find a home for this app. I want it to be supported in away it can remain open to its original mission of protecting a user's privacy. Institutional or hackivist support.

---------------------------------------------

# Summary
- **Scope**: Demo of Privy Loci is a privacy-first location inference app that allows users to manage their location data and subscriptions from willing third-party apps.
- **Usecases**: There are many. I will note ones I use. 
- **Requirements**: The prioritized functional and non-functional requirements ensure that the core goals of Privy Loci are met.
- **Documentation**: Categorizing documentation helps keep all the details organized, aiding in both current development and future maintenance.
- **Roadmap**: The roadmap focuses on delivering a PoT first, then gradually enhancing the app with more features and refinements.

# Scope of Privy Loci:
0. It is intended to be adopted by willing apps to develop location features that are privacy first in terms of location info.
1. PrivyLoci still needs access to regular location permissions for Android and iOS, it *will not* have a mock provider or bypass ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION in any way. It will instead provide strong guarantees through: Its privacy statement; it's not for profit commitment and institutional/activist support to provide a shield to never sell, collect or use this data in any which way for its own purposes outside of the user's own device.
2. The app does not intend to replace android's location and app permission management system, because there is no permission to apps to access location in the first place.
2. The app allows users and 3p apps to manage their data, for 3p apps this does not reveal PII or senstive info.
3. The app's privacy policy does not transmit this private location info. Certain means like encryption for this info means this info is always protected.

# Inference Use Cases: 
	1. It can be used by many or the user's apps to privately. For example:
		a. Set up geofences without explicitly asking for permissions.
		b. Location based reminders.

	2. It can tell an app where you parked your car when you disconnect from its BLE.
	3. It can tell an app when you are at home or at work or your favorite store.
	4. It can tell where your connected headphones were last you left them.
    5. An app could display a bunch of random location pins on a private map tile surface.
    6. There could be many more exciting usecases based on advanced algorithms running on device.

# Requirements

### 1. Functional and Non-Functional Requirements

#### **Functional Requirements (Prioritized)**

1. **Privacy Surface for Subscription Management (High Priority, Urgent)**
  - **Description**: Users should be able to view and manage all third-party app subscriptions through a central privacy surface (home screen).
  - **Key Features**:
    - Display all third-party apps with active subscriptions as tiles.
    - Allow users to expand each app tile to see and manage specific `(place/tag, event type)` subscriptions.
    - Users can delete individual subscriptions of apps directly from this view.

2. **App Event-Based Subscription Management (High Priority, Urgent)**
  - **Description**: Apps should be able to manage subscriptions tied to specific events like `GEOFENCE_ENTER` or `TRACK_BLE_ASSET_DISCONNECTED`.
  - **Key Features**:
    - Support subscription creation, deletion, and viewing via the Privacy Surface.
    - Log events related to subscription creation, deletion, and event triggers.

3. **User-Defined Places/Tags Management (High Priority, Urgent)**
- **Description**: Allow users to create, edit, and delete their own places/tags.
- **Key Features**:
    - Users can manage(add/delete/modify) their own set of Places/Tags and associate them with different events. 
    - Attach private metadata to these places like lat/long, Wifi, BLE ssids etc(NOTE: See encryption nf-req below).
      - Provide an appropriate UI(map) or a Wifi/BLE scan to achieve this.
    - Support for both user-created and app-created tags with an `ownershipType` enum.
  
4. **User-defined  Event Management (High Priority, Urgent)**
  - **Description**: Just like apps a user might want to create his own subscriptions for his own purposes like  `GEOFENCE_ENTER` or `TRACK_BLE_ASSET_DISCONNECTED`.
    - **Key Features**:
        - Users can create, edit, and delete their own events based on previously defined places and tags.
        - Privy Loc will thus function for the user for their bespoke usecases.
   
5. **Private Map Tile Display of PoI and locations (High Priority, Urgent)**
  - **Description**: Allow apps to show the user and their current location on a private map tile inside the app.
  - **Key Features**:
    - Apps have the ability a list of bespoke places and a geometry object to identify the map area to display.

6. **Apps should be able to extend using a simple plugin API**
    - **Description**: Allow apps to extend the functionality of Privy Loci by providing a plugin API. This is useful for a myriad number of use cases
   e.g. correct language and currency for region, regulatory purposes(can you operate in a country).
    - 
7. **Qibla Direction Calculation (High Priority, Important)**
  - **Description**: Provide a Qibla direction based on the user's current location without associating it with any place/tag.
  - **Key Features**:
    - A simple endpoint or function to calculate and return the Qibla direction.

8. **Logs Display (Medium Priority, Important)**
  - **Description**: Provide a simple UI for users to view logs of subscription creation, event triggers, and deletions.
  - **Key Features**:
    - Display logs in chronological order.
    - Include details like event type, associated app, and timestamp.

9. **User Onboarding (Low Priority, Important)**
  - **Description**: Implement a guided tutorial for first-time users to introduce the core features.
  - **Key Features**:
    - Highlight important components like the Privacy Surface, MainNavBar, and subscription management.
    - Use Android libraries (e.g., TapTargetView) for onboarding.

#### **Non-Functional Requirements (Prioritized)**

1. **Privacy and Security (High Priority)**
  - **Description**: Ensure that all user data, particularly location data, is handled securely and privately.
  - **Key Measures**:
    - Data encryption using Android APIs for all PII and sensitive data.
    - Strict control over data shared with third-party apps, ensuring no location data  is exposed.

2. **Compatibility and Flexibility (High Priority)**
  - **Description**: Ensure the app works across all Android devices, including those without Google Play Services.
  - **Key Measures**:
    - Use Android’s native APIs for location which can get us good fix. Use Fused Location Provider where Google.
    - Avoid reliance of Wifi/BLE Signature for direct lat/long inference where possible.
      - Example Wifi/BLE Signature -> Place/Tag inference is vastly preferable than Wifi/BLE -> lat/long like in WPS indexes (TODO: Document non trivial use cases where lat/long is indeed needed)

3. **Scalability and Performance (High Priority)**
  - **Description**: The app should perform efficiently even as the number of subscriptions and events grows.
  - **Key Measures**:
    - Provide a Privacy first efficient implementation of accurate location inference especially in urban dense areas. 
    - Optimize background processes related to Wifi scans, BLE scanning and location services(e.g. using motion apis). 
    - Efficiently manage data storage and retrieval for logs and subscriptions.

4. **Usability and Accessibility (Medium Priority)**
  - **Description**: Ensure the app is user-friendly and accessible to a wide audience.
  - **Key Measures**:
    - Simple, intuitive UI with clear navigation.
    - Consider localization and support for screen readers and other accessibility features in the future.

5. **Extensibility (Low Priority)**
  - **Description**: The app should be designed to allow easy addition of new features in the future.
  - **Key Measures**:
    - Modular design for features like encryption settings, data export, and advanced privacy controls.


### 3. Roadmap to Proof of Technology (PoT)(WIP)

#### **Phase 1: Core Functionality (PoT)**
1. **User-Defined Places/Tags Management**
    - Components: 
      - ~~**Data model**, A place/tag management activity[DONE].~~
        ```
        data class PlaceTag(
            val id: Int,
            val name: String,
            val type: PlaceTagType, // enum {PLACE, ASSET}
            val metadata: String, // Encrypted JSON string
            val createdAt: Long,
            val isActive: Boolean
        )

        enum class PlaceTagType {
            PLACE, ASSET
        }
        ```

      - The metatadata are details about the place like its lat/long for a place and Wifi/BLE SSID and additional metadata which the user might want to enter. All of it is encrypted. 
      - Asset Tracking Tag: Foreground Wifi and BLE scanning permission for asset tracking.
      - Place Tag: Map tile and GPS location display for Lat/Long based place.
2. **Subscriptions**
   - ~~Data model for user and 3p apps[DONE]~~:
       ```
       struct Subscription{
           subcription_id: int 
           type: enum{app,user}
           app_info: json
           place_tag_id: string 
           created_at: datetime 
           is_active: bool
           expiration_dt: datetime
       }
       ```
   - Each Place/Tags can have many Subscriptions. 
     - app_info are things like app name, app_id, app_logo, app_description etc if the type is app.  
3. **User-Defined Subscription Management**
    - Components: 
        - The subscription relates to an event using the subscription_id:
          - If there is no subscription for that app present 
        - API:
          - CRUD operations on subscriptions by apps/users.
          - When an user creates a subscription the notifications are in the pricy loci app itself.
    - Persistent and encrypted state tracking algorithms.

4. **3p App-Based Subscription Management**
    - Components: 
      - Simple intent based registration. 
      - App can get all its subscriptions.
      - App can delete all its subscriptions.
      - App can create a subcription and the API returns a subscription_id when a new subcription is created
        - App can create only N(=5) per place_tag.
        - App can only create M(=50) max subscriptions.
      
      - 3P-App can request to create a subcription for a user defined place/tag. User can accept or reject this.
        - To request this, the 3P-App must first ask for permission to access the list of a user's place/tags, without access to their location data.
5. **Events supported**
    - ~~The following events are supported[DONE]:~~
   ```
    enum class EventType {
        GEOFENCE_ENTRY,
        GEOFENCE_EXIT,
        TRACK_BLE_ASSET_DISCONNECTED,
        TRACK_BLE_ASSET_NEARBY,
        QIBLA_DIRECTION_PRAYER_TIME,
        DISPLAY_PINS_MAP_TILE
    }

    ```
      
6. **Privacy Surface for Subscription Management**
    - ~~Home screen[DONE]~~
    - ~~Display only for managing all subscriptions, grouped by app and user's places[DONE].~~
    - Logic for CRUD operations on subscriptions.
   
7. First pass for Geofence enter/exit event:
    - **Data model**:
      - ```
        struct GeofenceEvent{
          subscription_id: int,
          type: enum{GEOFENCE_ENTRY, GEOFENCE_EXIT},
          geofence_center: string, # lat/long
          geofence_radius: int, # in meters
          state: State
        }
      - The state field has the following structure:

        ```
        struct GeofenceState{
          enum most_recent_state, # alias: mrs. 0 for outside, 1 for inside, initialized based on event creation and updated by the algorithm
          long most_recent_state_change_ts, # alias: mrs_change_ts. When did the MRS last flip. Wall clock unix time.
        }
        ```
   
      - Map based selection interface.
      - Initialization of state for geofence and user for this event.
      - If there is subscription created implement an SMD interface to figure out when to collect location.

8. **Geofence Algorithm**:
    0. Permission display to ask user.  
    1. Initializethe State upon creation to be IN / OUT with the first accurate(<10m) LU. If there is no accurate LU
      or we can't collect, we could ask the user to tell us.
    2. Pseudocode for both entry and exit events with an additional debounce time:
   ```
   if (mrs == IN): # mrs is short for most_recent_state
     if (user is outside geofence):
       if (now.timestamp - mrs_change_ts > debounce_time):
         if event_type == GEOFENCE_EXIT:
             trigger_event()
         mrs = OUT  # Do these both after the event triggers in a callback. 
         mrs_change_ts = now.timestamp
   else:
     if (user is inside geofence):
       if (now.timestamp - mrs_change_ts > debounce_time):
         if event_type == GEOFENCE_ENTRY:
             trigger_event()
         mrs = IN # Do these both after the event triggers in a callback. 
         mrs_change_ts = now.timestamp
   ```
9.  For Wifi/BLE/IR Asset tracking logic(asset can connect or disconnect):
    - **Data model**:
        ```
        struct AssetTrack{
          subscription_id: int,
          type: enum{ASSET_TRACK_CONNECT_DISCONNECT, ASSET_NEARBY},
          asset_id: string,
          asset_name: string,
          asset_type: enum {BLE, WIFI, IR},
          asset_metadata: json, # encrypted
          state: State # encrypted
        }
        ```
    - Asset metadata is the Wifi/BLE SSID or MAC or unique identifier.
    - TODO(Sid): State model.
    - TODO(Sid): Add a simple connect/disconnect algorithm for tracking location of asset

10. Future Enhancements:
    - No GPS/Wifi: Tell user his indoor geofence may not be working ok.
    - Battery Opt: Consider implementing dwell time logic or something like " > 10 miles" logic to collect location update at a lower frequency.
    - Noise Reduction measures: Wifi/BLE + IMU + GPS SNR in a simple Naive bayes model for more accuracy indoors.
    - For high rises and really dense areas GPS SNR or Barometer could also be useful.

12. **Basic Logs Display**

#### **Phase 2: Enhanced Privacy Features**
1. **Private Map Tile Display**
2. **Qibla Direction Calculation**
3. **Encryption and Sensor Management (Planning for Future)**

#### **Phase 3: User Experience and Usability**
1. **User Onboarding**
2. **Improved Logs and Auditing**
3. **Accessibility and Localization**

#### **Phase 4: Future Enhancements**
1. **Data Export/Import Features**
2. **Advanced Privacy Settings**
3. **Scalability Enhancements**

-------------------------------------------