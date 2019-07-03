# How to contribute

We'd love to accept your patches and contributions to this project. There are just a few small guidelines you need to follow.

## Under which condition can you contribute

Contributions for this repository are made under the Repository License as described at 
https://help.github.com/en/articles/github-terms-of-service#6-contributions-under-repository-license.
This is the default terms of service on GitHub ("inbound=outbound").
That means for this this repository, the contributor agrees to provide a contribution under the Apache 2.0 license terms when uploading this contribution to this repository.

## What can you contribute

To improve the Qi Authentication Demo app, all kind of contributions are welcome except the following contributions:
- Source code which results into Qi Authentication messages which deviate from the Qi Authentication Specification
  - The goal of this demo app is the validation of Qi Authentication test implementations why any deviations from the definition in the Qi Authentication Specification conflict with this goal
- Source code which results into a changed NFC communication which is not backward compatible with the NFC communication used by previous versions of the Qi Authentication Demo app
  - Test implementations which were developed with a previous version of the Qi Authentication Demo app shall also work together with newer version of this app
