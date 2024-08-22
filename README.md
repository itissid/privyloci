Privy Loci: A Privacy First Location Inference API
===================================================

# Introduction
## The value proposition of Privy Loci begins with the recognition of a false choice.
The permissions tradeoff for location centric 3p apps on mobile platforms is based on a simple, but false choice: "Do you want this app to collect your location data? yes/no". Let's take an example to motivate the case. Say you want to find your headphones or keys that have a BLE dongle. At this moment this is done by:
1. Connecting/scanning for the BLE device and the lat long in the background and storing them both.
2. When you disconnect from BLE the app has the last location it shows it to you on a map.

But the 3p app does not **need** the sensitive location itself. A user eyes-only private service could do this tracking for the 3p app and just display a private map tile(via android intents or plugins) with a blue dot. This **separates** the Value Addition the 3p app gives - i.e. the Asset -  from the Private Location data. This is a 0-trust model and it can suffice many usecases, and is what I propose in a bit of detail here.

To go with an analogy, what most apps are forced to do is like asking a guy who rents an a room/floor in your house: "If you can't let me into your bedroom at any time, I will not rent it to you". An incredible invasion of privacy.

## Why do I call it a false choice?
To answer this, lets look a bit more at the specific choice you and app developers have today with location centric apps.

For users: YOU ARE FORCED TO CRINGE or AVOID,  a `1/0` choice (TODO: Permission on major mobile OS's is to collect or not collect. Expound on the fact that Android/iOS offer apps to collect location in foreground or background or always. Cue creepy iOS map on where all this app has collected your location. But all these choices are still variations of the same false choice.). I have seen first hand how privacy teams function in large orgs and I can tell you that no one reads the disclosures in privacy statements. 

Apps, even the good ones, are forced to exhibit creepy behaviour. (TODO: Apps with good intention or not *always* have to make a 1/0 choice between having a location or not, to function).  A lot of 3p applications(on android and iOS) build with features that rely on location don't need the actual lat/long, wifi SSID, ble SSID data to function. Many inferences can be made without collecting this data and shipping it off to servers. See the attached "Location Permission ambiguity in Android and iOS.pdf" for technical details of location permissioning on Android).

## What kind of problems it creates 

(TODO: Its not just that you don't get "value" from giving away location data  to big tech, bad as that is, you do get the very accurate Blue Dot and location services. But it is now being monopolized and the false choice has now led to the additional proliferation of location data beyond big tech to a myriad number of other players).
Problems with this model happens way too often in this world, for example, how horrified were you when a Muslim Prayer app data was bought by the FBI for god know what[2] or family location tracker app's data leaked[3]. Suffice to say location management is a shit show. I would go so far as to say it is to the benefit of Googles, Apples, Samsungs of this world to keep this shit show running as long as they can collect data all day and other apps can't.

# Privy Loci: An alternative privacy first model for location inferences with a PoC.
Privy Loci solves the problem of Privacy first Location *Inference* with a Zero Trust model.

## What is a privacy first location inference?

Note, I used the word Inferences not location data(accurate GPS position, IMU traces), I want to make clear that while current demo is demonstrating the API for protecting the privacy of the *inference* —  a higher order function like where is my BLE device? Are you home without telling the app where home is? —  it still needs all those mobile permissions to collect location data - that you don't like to give. The idea is to changes this, by spark a conversation about the permission structure and help create more private APIs. At this time all I can guarantee that all the location data collected by Privy Loci remains private and on device and encrypted on inception and never shipped to a server. 

My proposal is based on two observations when I worked in this domain previously:

1. Many inferences that rely location can be done on the device, with battle tested and well known existing algorithms. Most people spend ~80% of your time at Home or at Work and commuting between those two location. What if all this growing data not be used to improve privacy first inferences on devices? For e.g. geofencing around routinely visited places, location based reminders, Qibla direction can just tell the 3p app about certain *Events* like entering and exiting a fence or an area without needing them to necessarily access the location of interest or the center of the geo-fence itself. This app will enable all of those apps to function without expressly letting apps access the exact location.

2. What if you could use simple algorithms and techniques to do most inferences on device and privately? Simple clustering techniques, localized variance reduction  techniques(like Pooling Bayesian Inference, Gaussian Mixture models or Clustering techniques like DBScan) can reduce the uncertainty - and run on device -  for many inferences. Simple IMU data based regression/classification algorithms can solve the problem of Motion detection on device.  Dense urban areas are key, as phones become powerful and GPS becomes ever more accurate, on device inference should become the norm with newer inference methods[Shadow Matching and 3DMA: Paul Groves][Newer 3DMA work Stanford GPS LAB][zephr.xyz] that can be done on device or in a privacy-first manner, especially in dense urban areas. Large Wifi/BLE databases could still be made private[5].

Caveats: Many inferences and use-cases will still rely on post processing location accurately may still need to access and send location to a server to process. e.g. 911 calling(where accuracy and latency of the application is paramount), survey tools, regulatory/legal purposes where an device cannot legally function in XYZ country/place. There are also cases like place inferences, e.g. phototagging and traffic detection, that need different techniques to implement, yet I believe can be solved by federated learning and analogus anonymous p2p tech that, for example apple, uses for its near-by feature.

I am not naive and I do not hope that Android and iOS will magically change their location stack or permission structure, nor do I plan you to trust this app. I believe that with the right kind of organizational support from Privacy centric FOSS/non-profits Privy Loci will become a popular alternative and people and app developers could trust it. I want to keep it supported forever in its current envisioned form. To *hope* for changing the larger tech culture can change, though possible, is IMHO is not challenging the absurd[4] world.

# What PrivyLoci is not?
- I am not solving for accurate GPS positioning 
- An app to replace location infrastructure/services like those that use a combination of one or more of AGPS, Wifi/BLE databases, Fused Location providers[6]
- An app to replace all other location centric apps. On the contrary it empowers more of them to be created.

## But what about privacy for the location itself from big players in tech?

As we already noted, for Privy Loci to work our location data is still being collected by Apple, Google, Samsung or where ever it will run, because we have to use their APIs. I wish I did not have to. Despite predatory patent troll practices by players in this industry[7] that is crippling open alternatives like Mozilla Location Services and thus Android alternatives to play services like microG, one is forced to use them and at the moment they are the only ones to provide accurate, widespread, battery efficient inferences using a wrapper over A-GNSS, Wifi/BLE, Battery, IMUs[8] using what we call a "fused" provider[9]. To make these algorithms privacy first, one has to be incredibly focused on the use cases because the the world of location inferences really depends on what your target audience is and require great accuracy and cheap to be useful for 3p apps and users. 

Despite this, the world of location inferences is rich and there are several secondary inference techniques that could be done with Privacy, e.g. in AR/VR or positioning in urban and dense areas where AGNSS+Wifi/BLE+IMU fail. These are focusing around newer developments combining 3DMA, Deep learning using ever more accessible satellite image data for building models [10]. This is an exciting time to be in positioning.

As great as efforts like OSRM, OSMAnd and OpenStreetMaps are, they don't have parallel open source efforts to improve the blue dots. I think this is a mistake. Its partly because positioning has been perceived as infrastructure and that is hard to support in open source. But doing open source work to provide models and algorithms that are maintainable is possible and should be tried; We have already seen how deep transfer learning has been used to great effect in domains with strong inductive bias and how we now have a focus on open weights, open source in ML for the first time. 3DMA is already being adopted by Google[11] and Uber but it is expensive will soon be supplanted by newer techniques[10][12]. An exciting time as ever to be in positioning.

# (TODO) Privacy Model which is the guiding light for this app:
- What app purpose is solved by collecting *new* data?
- Is the data already being collected used differently?
- Does the user have control over their data?
- Are the users informed of what we are doing?


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