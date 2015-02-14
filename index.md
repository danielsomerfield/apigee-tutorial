---
title: About this tutorial
layout: default
---

The goal of this tutorial is to write a basic web service (a hello world variant, of course) using a continuous delivery pipeline, with code and configuration living in github (or an equivalent SCM).

### Project goals
- SSL-only: at no point in the call chain should a request be allowed over non-SSL
- Service-to-service auth: you should not be able to call the service other than through Apigee
- Automation: all configuration must go through github, including apigee endpoint configuration and heroku deployment information.

### Platform
- Build tool: gradle
- Service implementation: groovy running under Java 8
- Server: embedded jetty

### Caveats
- Because SnapCI doesn't allow you to export or check in pipeline
info (AFAIK) that will not be part of my source tree
- Private keys should never go in a public repo (arguably, any repo), so I'm not going to check them in. Although this is a challenging problem, I will leave it as a problem for another time. I will provide instructions for generating them. If located in the specified position on the file system, everything should just work.

### Assumptions
- You have an existing github account
- You have an existing heroku account
- You have an existing apigee account

  *A note on apigee accounts: apigee offers several services. Make sure you have the API Management service. You can sign up for
the free Developer unlimited trial. While it isn't enough for an enterprise deployment, it should provide you with
what you need for this tutorial.*
- You have an existing SnapCI account
- You have some level of knowledge of gradle and groovy. You don't have to be an expert (I'm not), but you are at least comfortable enough to understand the provided code
- You have Java 8 installed and on your path

The repo for the code is available on [GitHub](https://github.com/danielsomerfield/apigee-tutorial.git).
Along the way, tags will be listed in case you want to see the code in the state for the current step. At times, a single tag will serve for several tutorial steps.

**[Start the tutorial!]({{ site.baseurl }}/apigee-tutorial/1-setup-build-scripts.html)**
