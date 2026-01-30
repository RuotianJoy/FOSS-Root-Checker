<p align="center">
  <kbd>
  <img src="https://github.com/Chill-Astro/FOSS-Root-Checker/blob/master/Logo.png" width="128px" height="128px" alt="ROOT Checker">
  </kbd>
</p>
<h1 align="center"> FOSS ROOT Checker</h1>

FOSS Root Checker as the name suggests is an Open Source Root Checker app for verifying ROOT Access on Android Mobile Devices. Ever wondered what Root Checker Apps do behind the scenes on your Phones once you give them ROOT Access?

Well that's why I made this App! It is just a simple Root Checker for Newbies but with Transparency of what is done.

---

## How it works? 

The **FOSS Root Checker** employs a dual-layered verification strategy to determine system integrity without compromising your privacy:

1. **Functional Execution Check:** The app attempts to spawn a shell process to run the `su -c id` command. If the system returns a User ID of `0`, the app confirms that active SuperUser execution privileges are granted.
2. **Filesystem Signature Scan:** As a secondary fallback, the app performs a manual search through high-priority system paths (such as `/system/xbin/`, `/sbin/`, and `/data/local/`) for the presence of a standalone `su` binary.



By combining these methods, the app accurately detects root access across both legacy environments (Android 9/10) and modern implementations like **Magisk**, **KernelSU**, and **APatch**. All checks are performed on a background thread (`Dispatchers.IO`) to ensure your device remains responsive during the scan.

---
## Key Features :

- Privacy First Design with full transperancy.
- Modern Material UI.
- Support for Android 9+ Devices.
- Works with Magisk, KernelSU, APatch and any other method.
- Guidance provided on Rooting.

---    

## ⚠️ IMPORTANT NOTICE ⚠️

Please be aware: There are fraudulent repositories on GitHub that are cloning this project's name and using AI-generated readmes, but they contain **completely random and unrelated files in each release**. These are NOT official versions of this project.

**ALWAYS ensure you are downloading or cloning this project ONLY from its official and legitimate source:**
`https://github.com/Chill-Astro/FOSS-Root=Checker`

I am trying my best to report these people.

---

## Note from Developer :

Appreciate my effort? Why not leave a Star ⭐ ! Also if forked, please credit me for my effort and thanks if you do! :)

---
