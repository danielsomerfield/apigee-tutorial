#APIGEE Tutorial

The goal of this tutorial is to write a basic web service (a hello world variant, of course) using a continuous
delivery pipeline, with code and configuration living in github (or an equivalent SCM).

# Project goals
- SSL-only: at no point in the call chain should a request be allowed over non-SSL
- Service-to-service auth: you should not be able to call the service other than through apigee
- Authentication: the service will authenticate requests
- Automation: all configuration must go through github, including apigee endpoint configuration and heroku deployment information.

# Platform
- Build tool: gradle
- Service implementation: groovy running under Java 8
- Server: embedded jetty

There is a [step-by-step walk-through](http://danielsomerfield.github.io/apigee-tutorial/) *(in progress)* of the tutorial on the project website.

I encourage you to fork this project and make pull requests with improvements and corrections.
