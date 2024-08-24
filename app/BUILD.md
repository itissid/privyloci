
# How am I proceeding about this build:

0. First the usecases I can think of, with some examples
1. Next, I will list out the core Requirements for the application I am building.
2. I will build swimlane diagrams for the core requirements.
3. A roadmap with the datamodel and some of the key code components to build.
4. Once and only once built I want to properly message out and find a home for this app. I want it to be supported in away it can remain open to its original mission of protecting a user's privacy. Institutional or hackivist support.

---------------------------------------------

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
   e.g. correct language and currency for region, regulatory purposes(can you operate in a country), 
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
    - Components: Data model, A place/tag management activity.
      - place_tag_id, place_tag_name, place_tag_type, Place_tag_metadata, Created_At, is_active.
      
    - Asset Tracking Tag: Foreground Wifi and BLE scanning permission for asset tracking.
    - Place Tag: Map tile and GPS location display for Lat/Long based place.
    - 
2. **User-Defined Subscription Management**
    - Components: 
      - Data model(reused by 3p apps):
        - To track the subscriptions itself:
          - app_user_id, is_user_subscription, subcription_id, event_type, place_tag_id, created_at, is_active
          - Primary Key to identify a subscription is (app_user_id, event_type, place_tag_id)
        - To track the state of the subscription based on the event type we need to have different tables, specified below.
        - The subscription relates to an event using the subscription_id:
          - If there is no subscription for that app present 
        - API:
          - Create or delete a subscription. 
      - SMD and FLP. 
      - Persistent and encrypted state tracking algorithm.
    - Events supported: 
      - ARRIVE_AT_PLACE, LEAVE_PLACE, TRACK_BLE_ASSET, DISPLAY_MAP_TILE, QIBLA_DIRECTION_PRAYER_TIME.
    - For geofence enter/exit state management logic: 
      - Components: 
        - Data model:
          - 
          - subscription_id, geofence_center, geofence_radius,   
        - Map based selection interface.
        - Initialization of state for geofence and user for this event.
        - If there is subscription created implement an SMD interface to figure out when to collect location. 
        
      - First Algorithm Design(TODO: Flesh this out): 
        - Create Simple circular geofence with some arbitrary radius. 
        - Use SMD to figure out when to collect location using FLP.
        - Store a state of the user for the geofence and trigger based on change in state.
        - Add robustness by taking a moving average of the user's location with previous ones if the current location is < 10m of the geofence.
      - Future Enhancements:
        - Battery Opt: Consider implementing dwell time logic or something like " > 10 miles" logic to collect location update at a lower frequency.
        - Consider instead of SMD logic to collect locations more frequently when the user is moving and less frequently when the user is stationary. 
        - Privacy preserving: Experiment with Wifi/BLE signals and remove precise lat/long inference. 
          - Advantage: Wifi/BLE classifier is possibly as privacy preserving if not more than lat/long inference. User can have a private wifi/ble beacon at these places.
          - If Wifi/BLE is strong enough when user enters geo-fence, consider using wifi and geo location in a naive bayes classifier setting to classify in/out instead.
    - For BLE Asset tracking logic(asset can connect or disconnect):
      - Add enum type for 


3. **3p App-Based Subscription Management**
    - Components: 
      - New Enum type in 
4. **Privacy Surface for Subscription Management**
5. **BLE Asset Tracking**
6. **Basic Logs Display**

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

### Summary

- **Requirements**: The prioritized functional and non-functional requirements ensure that the core goals of Privy Loci are met.
- **Documentation**: Categorizing documentation helps keep all the details organized, aiding in both current development and future maintenance.
- **Roadmap**: The roadmap focuses on delivering a PoT first, then gradually enhancing the app with more features and refinements.

Would you like to refine any of these sections further, or should we proceed with the next steps?



## There are certain Non-functional requirements Privy Loci fulfills:
    - The app will use reasonable APIs to encrypt the location data on device. The data never leaves the device.
    - It never accesses the internet unless it needs to — so there is no doubt as to the transmission of your location data [1].

# Service Design
Follow the Requirements to suggest the modules that need to be built, starting with basic components, including but not limited to:
1. Foreground service.
2. The app's classes, data model.
3. Subscription API design for 3p Apps.
4. User's own interaction use-cases like management of tags and apps that have subscriptions.

-------------------------------------------