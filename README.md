# Temperature Alarm

Temperature and humidity are reported to a [Google App Engine (GAE)][1]
backend, which notifies [Android][2] devices via [Firebase Cloud Messaging][3]
if the temperature moves outside of the allowed range. The data reported from
the sensor is stored on [Firebase][4], which allows for real time updates on
devices.

## License

    Copyright 2014-2017 Gregory S. Meiste

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[1]: https://cloud.google.com/appengine/
[2]: https://www.android.com/
[3]: https://firebase.google.com/docs/cloud-messaging/
[4]: https://firebase.google.com/