![Logo](/images/qi_ath.png)

# Qi Authentication Demo

Demo app to verify and analyze Qi Authentication message exchange at Android phones

## Functions

This demo app provides the following functions:
- Emulates a PTx devices able to be authenticated by an emulated PRx device
  - Emulates at default four different PTx devices
    - One PTX devices has a Secondary Certificate in its Certificate Chain
    - One PTx device simulates a Feke device to test error behaviour
    - PTx Devices from different Manufacturers are emulated
  - Uses NFC communication to respond to Qi Authentication message requests
- Emulates a PRx devices requesting a Qi Authentication from the emulated PTx device
  - Three different Protocol flows are emulated
  - Two Protocol flows emulate cach memory for PTx devices
  - Uses NFC communication to send Qi Authentication messages requests
- Analyses and verifies WPC Certificate chains used for a virtual plugfest
- Analyses and verifies CHALLENGE request and CHALLENGE_AUTH response pairs used for a virtual plugfest
- Analyses and verifies WPC Root and Manufacturer Certificates
- Performs the calculation of the example Certificate Chain used in the Qi Authentication specification
- Supports the storage of logged exchanges or analysis

If this app is installed on two different NFC enabled Android phones, then a Qi Authentication can be demonstrated between these two phones by using NFC communication: 
- One phone is configured to emulate a PTx device
- The other phone is configured to emulate a PRx device

The purpose of this demo app is the validation of Qi Authentication test implementations and *will not work together with real Qi charging devices.*

This app uses the [Spongycastle library](https://github.com/rtyley/spongycastle) distributed under a [license based on the MIT X Consortium license](https://github.com/rtyley/spongycastle/blob/spongy-master/LICENSE.html). The Spongycastle library itself includes a modified BZIP2 library which is licensed under the Apache 2.0 license. 

## Installation

To install this demo app read [Installation](wiki/Installation) and read the [User Manual](wiki/Home).

## Contribution

If you want to contribute to this repository, then read first [CONTRIBUTING.md](CONTRIBUTING.md).
