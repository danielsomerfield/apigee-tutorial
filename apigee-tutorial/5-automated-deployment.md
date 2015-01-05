---
title: Automated Deployment
layout: tutorial
tag: setup-snap-ci
---

*** IN PROGRESS ***


In [the last section](4-setting-up-snap-ci.html), we enhanced our build and created a pipeline in Snap so our tests would run on every commit. Now we move to the next stage and push our code out to the live server. So, I'm going to start with some caveats.

Heroku is great in many ways. It's VERY easy to pull code off your public repo, build it (if it isn't too large) and then have it deployed within minutes. It's free for small test instances, and seems fairly affordable. But, honestly, it doesn't seem to be built for compiled apps or for large continuous delivery deployments for the following reasons:

- the path of least resistance is to pull your source code from git, build it on the heroku machine and run. This means burning cycles (and dollars) building your app again. Maybe not a big deal if you are in ruby, but not ideal in Java. It also means for Java that we would be breaking the sacred continuous deployment rule of not re-building our binaries.
- there is a time-limit on builds. If you're app doesn't build in under 15 minutes, it's going to time out. A 15 minutes build is, admittedly pretty long, but if you are pulling down a lot of artifacts over the big, bad, unpredictable Internet, this isn't out of the question. A bunch of small microservice are probably fine, but a big monolithic app wouldn't be ideal.
- which brings us to another point. This is really meant for public repos, or possibly private repos on a public site. If you have a private repository behind your firewall, I expect you are out of luck.

So here's what we're going to do: we built that zip file, so we're going to use it.

### Setting up Heroku
Ignoring all the above caveats, deploying this to Heroku is so easy it's almost unfair. The steps are:

- Create a new heroku app
- Add a staging target to our gradle
- Add a

*** IN PROGRESS ***

<!--
## [Continue to "Section 6: Apigee Deployment"](6-apigee-deployment.html) ##
-->
