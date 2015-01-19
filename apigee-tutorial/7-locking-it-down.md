---
title: Apigee Deployment
layout: tutorial
tag: apigee-deployment
---
In [the last section](6-apigee-deployment.html), we added Apigee to our pipeline. Now we can work with that full stack pipeline to add cool features to our product.

When we configured the final step in our pipeline, we ran our smoke test again the url `http://`*myhost*`-prod.apigee.net` but our application is in the clear. There is nothing to prevent snoops and hackers from stealing our precious data. So here's what we want to do:

- Secure the transport between the client and Apigee
- Secure the transport between Apigee and the Heroku application
- Lock down application access to our target users

One note on securing services: do not deploy an application with sensitive security needs to a public host, then lock it down. You don't ever want to leave it susceptible to attack. If you need to test your security mechanism, roll out "hello world" over your pipeline, write tests that assert the security requirements, then roll out your actual application endpoint with the security mechanism in place. That way your code will be well-tested, you will be practicing good continuous delivery by delivering MVPs, but you won't leave yourself open to an attack while you build up the security.

## Securing the Transport to Apigee
When we configured the proxy bundle, we used the default virtual host, which means our connection is over http. Luckily this is **really** easy to fix. If we never want our tests to fail, we would have to deploy a second host, but, personally, I'd rather update the assertion in the tests, let it fail, then deploy the fix.

I want to assert two things in my test:

- I can connect to the endpoint over SSL
- I can't connect to the endpoint if I'm NOT over SSL

This means we need to re-work our tests a little bit.
...
<!--
## [Continue to "Section 8: Locking it Down"](8.html) ##
-->
