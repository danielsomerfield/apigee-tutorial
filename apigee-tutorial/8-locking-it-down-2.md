---
title: Apigee Deployment
layout: tutorial
tag: apigee-deployment
---
# In Progress #


In [the last section](7-locking-it-down.html), we locked down the transport by forcing all connections to be via SSL. Now we want to make sure that all requests come through Apigee, so that we can use Apigee security or rate limiting features. We have one challenge due to our use of Heroku--actually it's a challenge because Heroku is on AWS: elastic load balancers (ELBs) terminate SSL. That means that we are unable to get the client credentials presented by Apigee. Otherwise we could just authenticate the connected based on the Apigee client certificate and we'd be done. Sadly, that won't work for us. If you are deploying in own data center and you don't have SSL-terminal load balancers in the way, this is probably the way to go.

In our case, we will need to consider different mechanisms. At this point, the decision should come down to the needs of the application. A higher degree of security comes with additional complexity. For example: our application is over SSL, which means we are not really susceptible to replay attacks from attackers between Apigee and our backend service, but we could be susceptible to a replay attack from inside the Heroku network. If we were dealing with sensitive financial data, perhaps that would be a concern, but we don't need that level of security. Heroku is multi-tenant but each tentant is isolated by firewall routing. Unless they screw up their routing rules, we should be ok. For our app, that's enough.

So what are our choices here:
- OAuth-based authentication
- SAML-based authentication
- Validation of an API key
- Shared secret
- Shared secret with cryptographic validation
- PKI-based validation


# In Progress #



<!--
## [Continue to "Section 8: Locking it Down Part 2"](8-locking-it-down-2.html) ##
-->
