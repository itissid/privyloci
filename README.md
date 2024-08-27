Privy Loci: A Privacy First Location Inference API (Demo: September'24) 
===================================================

Privy Loci is an proposal for:

a. Privacy First Location *Inferences* with a Zero Trust model for users.

b. Building 3p apps that rely on location data to without sacrificing function.

NOTE: I refer to "location data" to include not just (Lat,long,accuracy) tuple, but also RF: BLE/WiFi data, Pose, Activity Recognition data etc. Privy Loci's demo focuses on the Lat/Long and RF data. 

## TL;DR How does it work?
It proposes a complete *separation* of the PII and sensitive location data from the *value* that apps add to your life.
It is also compatible, due to an open design, to let users and 3p-apps to work across platforms to provide a privacy-first location inference API.  
To think that the idea is, “well, isn't this just a technical issue like differential privacy or e2e-encryption?” misses the point a bit. While those are tactics to implement privacy, one has to think from first principles to solve the real problem which is deeply rooted in the Location Permission Structure and the designs built on it in major platforms, as I explain below. This is a major concern and can be mitigated for a good number of apps to function while not letting users deal with the worry.

# Is it meant to be an App? How can I use it?
Not exactly. It is an app form for the demo and usable, for example to track assets and set private geofences, though I would rather see it as being a core service an Infrastructure as a Service(IaaS) model to enable users and long term support(LTS) for 3p-apps.
I want to give you a feel for what it could be like. 

The bigger idea is to build a foundational document rethinking the problem and core algorithms
to enable such things. FOSS, Privacy Minded Institutional and Hackivist support is 100% welcome. 

# What PrivyLoci is not?
- In this demo I am not solving for accurate private GPS positioning. But this is entirely possible and pivotal to gaining complete privacy. At the moment I do use Google's Fused Location Provider[6] to demo.
- It is not an app to replace location infrastructure/services like those that use a combination of one or more of AGPS, Wifi/BLE databases[6]. Though building open but private Wifi/BLE networks and is possible[5] it is not the only way to provide accurate inference[12][11].
- It is not an app to replace other location centric apps. On the contrary it empowers more of them to be created.

# Introduction
## The value proposition of Privy Loci begins with the recognition of a false choice.
The permissions' tradeoff for location centric 3p apps on mobile platforms is based on a simple, but false choice presented to the user: **“Do you want this app to collect your location data? Yes/No”**. If you say yes, you have no easy to determine privacy guarantees, if you say no you can't use these apps at all. Let's take an example to motivate the case. Say you want to find your headphones or keys that have a BLE dongle(see my FAQ about Apple below on this). At this moment, this is done, approximately, by:
1. Connecting/scanning for the BLE device(known list or auto connect) and the lat long in the background and storing them both.
2. When you disconnect from BLE, the 3p-app has the last location it shows it to you on a map.

But the 3p app does not **need** the sensitive location data itself. A user eyes-only Private Service could do this tracking for the 3p app and just display a Private Map tile, like I will show in Privy Loci(via android intents or plugins) with the blue dot. This **separates** the Value Addition the 3p app gives — i.e., the Asset —  from the Private Location data. The look and feel like map tiles, PoIs etc could be customized per the app's themes. 
This is a 0-trust model, and it can suffice many use cases, and is what I propose in a bit of detail here.


To go with an analogy, what most apps are forced to do is like asking a guy who rents a room/floor in your house: “If you can't let me into your bedroom at any time, I will not rent it to you”. An incredible invasion of privacy.


## Why do I call it a false choice?
To answer this, let's look a bit more at the specific choice you and app developers have today with location centric apps.


1. For users: YOU ARE FORCED TO CRINGE or NOT use the app at all,  a `1/0` choice:


   Permission on major mobile OSes is to collect or not collect. Expound on the fact that Android/iOS offer apps to collect location in foreground or background or always. (TODO: Cue creepy android/iOS map on where all this app has collected your location). But all these fine-grained permissions are still variations of the same false choice. I have seen first hand how privacy teams function in large orgs function and I can tell you that though well-intentioned data misuse is rampant because these policies can be changed to provide “value”.


2. For 3P-Apps, even the virtuous, privacy-conscious ones, are forced to exhibit creepy behavior to the user or not exist:


   Apps with good intention or not almost *always* have to make a 1/0 choice between having a Location data or not, to function. A lot of 3p applications(on android and iOS) build with features that rely on location **don't need** the actual lat/long, Wi-Fi SSID, BLE SSID data to function. Many inferences can be made without collecting this data and shipping it off to servers.

## What kind of problems it creates

It's not just that you don't get value from giving away location data to major OS platforms, bad as that is, you do get the very accurate Blue Dot and a host of location services. But these services are now being monopolized, and the false choice has now led to the additional proliferation of location data beyond major mobile platform OS providers to a myriad number of other smaller players. In other words its a Product and System Design problem and not just an engineering one.


Problems with this model happens way too often in our world, for example, how horrified were you when you learned that a Muslim Prayer app data was bought by the FBI for god know what[2]? Or that a family location tracker app's data leaked[3]? Suffice to say, location management is a shitshow. I would go so far as to say it is to the benefit of Googles, Apples, Samsungs of this world to keep the status quo running as long as they can collect data all day and other apps can't.

## What is a privacy first location inference?

Note, I used the word Inferences not location data (accurate GPS position, IMU traces), I want to make clear that while current demo is demonstrating an API for protecting the privacy of the *inference* —  a higher order function like where is my car? Are you at home without telling the app where home is? —  it still needs all those mobile permissions to collect location data that you don't like to give. The whole point is to changes this and ultimately to do accurate private location inferences. 

For now its a way to spark a conversation about the state of things that can enable more private APIs. At this time all I can guarantee that all the location data collected by Privy Loci, should you use it, would remain private and on device and encrypted on inception and never shipped to a server. Future versions will propose an anonymized infrastructure design to do post processing as well as privacy preserving algorithms that are cross platform and can handle expensive computation.

My proposal is based on two simple observations when I worked in this domain previously(nothing earth shattering about these):

1. Many inferences that rely on location can be done on the device, with battle tested and well known existing algorithms. Most people spend ~80% of your time at Home or at Work and commuting between those two locations. All this growing data can be used to improve privacy first inferences on devices(See my spiel about apple's equivalent Significant Location in the FAQ below). For e.g. geofencing around routinely visited places, location based reminders, Qibla direction can just tell the 3p app about the event and not disclose the sensitive info, while not refusing them permissions. Like entering and exiting a fence or an area without needing them to necessarily access the location of interest or the center of the geo-fence itself. An adoption of privy loci's model will enable all of those apps to function without privacy concerns, forever and across platforms.


2. What if you could use simple algorithms and techniques to do most inferences on device and privately? Simple clustering techniques, localized variance reduction techniques(like Pooled Bayesian Inference, Gaussian Mixture models or clustering techniques) can reduce the uncertainty — and run on device —  for many inferences. Simple IMU data based regression/classification algorithms can solve the problem of Motion Detection on the device. Dense urban areas are a concern, but as phones become powerful and GPS becomes ever more accurate, on device inference should become the norm with newer inference methods [Shadow Matching and 3DMA: Paul Groves](https://profiles.ucl.ac.uk/6850), [3DMA NLOS, Stanford GPS LAB](https://www.ion.org/publications/abstract.cfm?articleID=19403), [zephr.xyz](zephr.xyz) that can be done on device or in a privacy-first manner, especially in dense urban areas. Large Wi-Fi/BLE databases can be made anonymous when queried[5] where needed to improve indoors accuracy.


Caveats: Many inferences and use-cases will still rely on post-processing location accurately may still need to access and send location to a server to process. e.g. 911 calling(where accuracy and latency of the application is paramount), survey tools, regulatory/legal purposes where a device cannot legally function in XYZ country/place. There are also cases like place inferences, e.g. phototagging and traffic detection, that need different techniques to implement privacy, yet I believe, they can be solved by additional techniques like federated learning and analogous anonymous p2p tech that, for example apple, uses for its near-by feature.


I am not naive, and I do not hope that Android and iOS will magically change their location stack or permission structure, nor do I plan you to trust this app. I believe that with the right kind of organizational support from Privacy centric FOSS/non-profits, Privy Loci will become a popular alternative and people and app developers could trust it. I want to keep it supported forever in its current envisioned form. To *hope* for changing the larger tech culture can change, though possible, is IMHO is not challenging the absurd[4] world.

I am not naive and I do not hope that Android and iOS will magically change their location stack or permission structure, nor do I plan you to trust this app. I believe that with the right kind of organizational support from Privacy centric FOSS/non-profits Privy Loci will become a popular alternative and people and app developers could trust it. I want to keep it supported forever in its current envisioned form. To *hope* for changing the larger tech culture can change, though possible, is IMHO is not challenging the absurd[4] world.



## But what about privacy of the location data itself from big players in tech?

As we already noted, for Privy Loci to work our location data is still being collected by Apple, Google, Samsung or where ever it will run, because we have to use their APIs. I wish I did not have to. Despite predatory patent troll practices by players in this industry[7] that is crippling open alternatives like Mozilla Location Services and thus Android alternatives to play services like microG, one is forced to use them and at the moment they are the only ones to provide accurate, widespread, battery efficient inferences using a wrapper over A-GNSS, Wifi/BLE, Battery, IMUs[8] using what we call a "fused" provider[9]. To make these algorithms privacy first, one has to be incredibly focused on the use cases because the the world of location inferences really depends on what your target audience is and require great accuracy and cheap to be useful for 3p apps and users. 

Despite this, the world of location inferences is rich and there are several secondary inference techniques that could be done with Privacy, e.g. in AR/VR or positioning in urban and dense areas where AGNSS+Wifi/BLE+IMU fail. These are focusing around newer developments combining 3DMA, Deep learning using ever more accessible satellite image data for building models [10]. This is an exciting time to be in positioning.

As great as efforts like OSRM, OSMAnd and OpenStreetMaps are, they don't have parallel open source efforts to improve the blue dots. I think this is a mistake. Its partly because positioning has been perceived as infrastructure and that is hard to support in open source. But doing open source work to provide models and algorithms that are maintainable is possible and should be tried; We have already seen how deep transfer learning has been used to great effect in domains with strong inductive bias and how we now have a focus on open weights, open source in ML for the first time. 3DMA is already being adopted by Google[11] and Uber but it is expensive will soon be supplanted by newer techniques[10][12]. An exciting time as ever to be in positioning.

# (TODO: Improve me) Privacy Model which is the guiding light for app design:
- What app purpose is solved by collecting *new* data?
- Is the data already being collected used differently?
- Does the user have control over their data?
- Are the users informed of what we are doing?
# Other questions you may have
## Q. Doesn't Apple already provide a very good privacy model for location data?
  Apple advertises privacy in a very upfront manner[13][16]. If you believe them and you can afford it, more power to you. There are indeed some good products, for example they mention that in the Find My network which uses nearby apple to locate yours, they encrypt the data. They also mention several noteworthy features which are in the spirit of Privy Loci[14] for example:

  - On-Device processing of location data where possible.
  - Individual control of what apps have access to Bluetooth data in as recent as iOS 13 and limit preciseness of such inferences later.    
     
But instead of enumerating extensive privacy guarantees and comparing them to Privy Loci. Lets look at the initial premise of Privy Loci - the False Choice. Can 3P apps, for example, work and render their experience without getting into the false choice trap? Here the evidence is weaker [15], an app could render inferences about finding your car by invoking the map Privacy Surface like in Privy Loci, where the user is assured of privacy guarantees, instead iOS limits location access[15][17].  
    The bigger issue really is the so called Walled Garden of apple. FindMy, iBeacons all work on Apple devices and to serve its ecosystem. Even if take the privacy guarantee on its face, Apple can wall off features as it likes by
    doing whatever it wants to the competition. 


# Credits
## Icon artwork from The Noun Project:
- Home by il Capitano from <a href="https://thenounproject.com/browse/icons/term/home/" target="_blank" title="Home Icons">Noun Project</a> (CC BY 3.0)
- New by Alice Design from <a href="https://thenounproject.com/browse/icons/term/new/" target="_blank" title="new Icons">Noun Project</a> (CC BY 3.0)
- Action Overflow by Trevor Dsouza from <a href="https://thenounproject.com/browse/icons/term/action-overflow/" target="_blank" title="Action Overflow Icons">Noun Project</a> (CC BY 3.0)
- Private Location by Soremba from <a href="https://thenounproject.com/browse/icons/term/private-location/" target="_blank" title="Private Location Icons">Noun Project</a> (CC BY 3.0)

-----------------------
# References
[1] Though, this means that we can't use Google's fused provider either for more accurate location, but there is a longer term plan for this.

[2] https://www.vice.com/en/article/muslim-app-location-data-salaat-first/

[3] https://hackread.com/family-location-tracker-app-life360-breach-data-leak/

[4] https://www2.hawaii.edu/~freeman/courses/phil360/16.%20Myth%20of%20Sisyphus.pdf

[5] https://github.com/wiglenet/m8b

[6] https://stackoverflow.com/a/6775456/247336

[7] https://news.ycombinator.com/item?id=39724505

[8] https://stackoverflow.com/a/46671962/247336

[9] https://android.googlesource.com/platform/frameworks/base/+/master/packages/FusedLocation/src/com/android/location/fused/FusedLocationProvider.java

[10] https://www.ion.org/publications/abstract.cfm?articleID=19403

[11] https://insidegnss.com/end-game-for-urban-gnss-googles-use-of-3d-building-models/

[12] https://www.zephr.xyz/

[13] https://www.apple.com/privacy/features/
[14] https://www.apple.com/privacy/docs/Location_Services_White_Paper_Nov_2019.pdf
[15] https://radar.com/blog/understanding-approximate-location-in-ios-14
[16] https://www.apple.com/legal/transparency/pdf/requests-2023-H1-en.pdf
[17] https://stackoverflow.com/questions/76504194/will-location-summary-for-always-allow-location-permission-shown-for-ibeaconre
[18] https://github.com/wiglenet/m8b