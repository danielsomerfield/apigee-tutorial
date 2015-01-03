---
title: Setting Up Build Scripts
layout: tutorial
tag: write-your-service
---
First thing, before we write any code, is to set up the build environment so we can build, test and deploy the code on our local machine in an automation fashion. I have chosen to use gradle because:

- it's fairly easy to use out of the box
- it's becoming a more and more common platform for this kind of product
- it's much easier to extend than something like maven
- as a Java coder, I can work it in efficiently

By the time we're done, the code should be structured as follows:
{% highlight bash %}
gradle/
  wrapper/
    gradle-wrapper.jar
    gradle-wrapper.properties
.gitignore
build.gradle
gradlew
gradlew.bat
settings.gradle
{% endhighlight %}

I won't go into any great detail on the gradle wrapper classes. If you want to learn more about those, and why they are there, please read up on gradle on the [gradle docs pages](http://www.gradle.org/docs/current/userguide/gradle_wrapper.html). Suffice it to say that for a continuous integration project, we are better having the build resources within our project than counting on what exists on the file system.

The most interesting file is the build.gradle:
{% highlight groovy %}
apply plugin: 'groovy'

sourceCompatibility = 1.8
version = '1.0'

repositories {
  mavenCentral()
}

dependencies {
  testCompile 'junit:junit:4.+'
}

test {
  exclude '**/*UATest.*'
}

task uat (type: Test, dependsOn:'test') {
  include '**/*UATest.*'
}

task wrapper(type: Wrapper) {
  gradleVersion = '2.1'
}
{% endhighlight %}
*[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/write-your-service/build.gradle)*

It's all pretty standard gradle stuff except:

- I have overridden the test task to exclude everything with "UATest" in the class name.
- There is a custom task "uat" which will run only test classes with "UATest" in the name.
- I have included the "wrapper" task which initializes the gradle wrapper. Once that is checked in, it's arguable with this task is necessary anymore, but I left it in for posterity.

The purpose here is so that we can run our unit tests (via ./gradlew test) and our user acceptance tests (via ./gradlew uat) separately. You can do that now although the results will be less than thrilling since we don't have any code at this point.

Speaking of which, as good TDD developers, the next thing we're going to do is write a test that exercises our first use case.

## [Continue to "Section 2: Write Your Acceptance Test"](2-write-your-acceptance-test.html) ##
