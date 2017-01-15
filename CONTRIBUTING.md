Contributing
============

#### Would you like to contribute code?

1. [Fork Temperature Alarm][1]. See further setup instructions below.
2. Create a new branch ([using GitHub][2] or the command `git checkout -b descriptive-branch-name dev`)
3. Make [great commits + messages][3].
4. [Start a pull request][4] against `dev`. Reference [existing issues][5] when possible.

When submitting code, please make every effort to follow existing conventions
and style in order to keep the code as readable as possible.

#### Do not want to code?
* You can [suggest features][5].
* You can [discuss a bug][5] or if it was not reported yet [submit a bug][6]!

Branch structure
----------------

The repository is made up of two main branches:

* `master` has the latest stable code
* `dev` is the main development branch

Setup
-----

This project is built with Gradle, and the [Android Gradle plugin][7].

1. Clone this repository inside your working folder.
2. Setup a project on the [Firebase Console][8] and download the generated
   google-services.json file to the app directory.
3. In [Android Studio][9], import the build.gradle file in the root folder.

 [1]: https://github.com/meisteg/RaspberryPiTempAlarm/fork
 [2]: https://help.github.com/articles/creating-and-deleting-branches-within-your-repository/
 [3]: https://robots.thoughtbot.com/5-useful-tips-for-a-better-commit-message
 [4]: https://github.com/meisteg/RaspberryPiTempAlarm/compare
 [5]: https://github.com/meisteg/RaspberryPiTempAlarm/issues
 [6]: https://github.com/meisteg/RaspberryPiTempAlarm/issues/new
 [7]: https://developer.android.com/studio/releases/gradle-plugin.html
 [8]: https://console.firebase.google.com/
 [9]: https://developer.android.com/studio/index.html
