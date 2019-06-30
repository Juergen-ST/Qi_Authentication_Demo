# Contributing guideline

To improve the Qi Authentication Demo app, all kind of contributions are welcome except the following contributions:
- Source code which results into Qi Authentication messages which deviate from the Qi Authentication Specification
  - The goal of this demo app is the validation of Qi Authentication test implementations why any deviations from the definition in the Qi Authentication Specification conflict with this goal
- Source code which results into a changed NFC communication which is not backward compatible with the NFC communication used by previous versions of the Qi Authentication Demo app
  - Test implementations which were developed with a previous version of the Qi Authentication Demo app shall also work together with newer version of this app
