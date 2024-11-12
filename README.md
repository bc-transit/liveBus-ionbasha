# liveBus
An Android app which allows users to track their bus in real time!

Made using Kotlin language in the Android Studio IDE.

Used Google Maps SDK to display map, and used TransLink REST API to gather bus data
(i.e. bus longitude/latitude, direction, final destination.)


------

# Link to the rttiapi code

    $ grep -r rttiapi *
    app/src/main/java/com/example/translinktrack/MainActivity.kt:\
    var URL = "https://api.translink.ca/rttiapi/v1/buses?apikey="+translinkAPIKey+"&routeNo="+input


