# Arpokrat Privacy Policy

**Last Updated:** May 8, 2026

**Arpokrat GmbH** **Address:** c/o Wadsack AG, Bahnhofstrasse 7, CH-6302, Zug, Switzerland.

---

## 1. Our Philosophy: Anonymity and Sovereignty

To access our messaging application, you do not need a phone number, and creating a user account is not required. We are committed to ensuring your absolute confidentiality and are dedicated to your effective digital sovereignty.

The fundamental rule of Arpokrat is strict: **we do not collect any usage data.**

The only exception concerns the purchase of licenses (ArpokratOS): during billing, we only collect your name, email address, and country of residence. This data is collected exclusively to comply with Swiss tax obligations. It is stored in an isolated manner for 10 years and then permanently deleted.

Your personal messages and calls are end-to-end encrypted. No one, not even Arpokrat, can read or listen to them, nor can they access the files sent and/or received.

## 2. Specifics of ArpokratOS (Operating System)

If you use our secure operating system (ArpokratOS, optimized for Google Pixel devices) in addition to our messaging application, your protections are enhanced at both the hardware and software levels:

* **Location Disabled:** The OS blocks access to GPS tracking modules. You cannot be geolocated, and you cannot share your location, as this function is neutralized at the root level.
* **NFC and Bluetooth (BT) Disabled:** Near-field communication (NFC) and Bluetooth protocols are disabled at the system level to prevent any relay attacks, remote scanning, tracking, or unauthorized pairing attempts without your knowledge.
* **Data Sovereignty:** ArpokratOS enforces privacy by default by disabling physical and location attack vectors at the Kernel level. This technical measure ensures compliance with "Data Protection by Design" (Art. 25 GDPR / Art. 7 FADP), physically preventing any unauthorized data collection and securing the device against data extraction.

## 3. The Only Information You Provide (Billing)

You do not need to provide any personal information to use our messaging application. You are identified only by a cryptographic key generated locally on your device.

* **License Purchases (ArpokratOS):** If you choose to purchase a license for our OS, we will only ask for your name, email address, and country of residence. This information is collected exclusively to strictly comply with current tax regulations in Switzerland and to issue the corresponding invoice.
* **Storage and Isolation:** This billing data is stored in encrypted form on administrative servers located in Switzerland, completely isolated and disconnected from the communication infrastructure (which is "Zero-Log").
* **Retention:** In accordance with the Swiss Code of Obligations, this accounting data is retained for 10 years and then securely destroyed. It is never cross-referenced with your cryptographic activity or communications.

## 4. Processing of User Content (Communications)

We provide asymmetric end-to-end encryption (E2EE) for all our Services. Your calls, messages, group metadata, and multimedia files (images, audio, video, documents) are encrypted at the source.

* **Routing in Volatile Memory (RAM-Only):** Your messages are stored exclusively on your device(s) and not on our servers. To ensure transmission from sender to recipient, our relay servers operate solely in random-access memory (RAM). No communication data is written to a physical hard drive. Once the message is delivered to the target device, the encrypted packet is instantly erased from our servers' memory.
* **Pending Messages:** If a recipient is offline and a message cannot be delivered immediately, we store the encrypted packet on our transit servers for a maximum of 30 days to attempt delivery. After this period, if it has not been delivered, the message is permanently purged. As it is encrypted, the content of this pending message is strictly unreadable by us.
* **Multimedia Files:** When a user sends large files, we temporarily store these encrypted packets for a maximum of 30 days to optimize their distribution (for example, for transfers within a group). We cannot view these files or extract their metadata (EXIF).
* **Groups:** You can create, name, and add descriptions or photos to your groups. All this information is also end-to-end encrypted. We do not collect any information regarding the member list, the group size, or its creation or update date.

## 5. Information We DO NOT Collect

To remove any ambiguity, here is the information that our technical architecture is strictly designed to never collect, analyze, or store:

* **Usage Information:** We do not know how often, at what time, or with whom you use our Services.
* **Performance and Troubleshooting Information:** We do not collect any diagnostics, error logs (crash logs), or performance data.
* **RAM-Only Infrastructure and No Logs (Zero-Log):** Our communication relays (AMP, TURN, xFTP) operate exclusively in random-access memory (RAM). No metadata or connection logs are persistently stored on our hard drives. In accordance with the data minimization principle (Art. 5 of the GDPR), encrypted data packets are destroyed immediately after successful transmission.
* **Device and Connection Information:** We do not collect your IP address, device model, battery level, signal strength, or router identifier.
* **Location:** We do not collect any geolocation data (GPS, Bluetooth, or Wi-Fi access points).
* **Cookies, Trackers, and Analytics Tools:** Our public website (arpokrat.com), our application, and our services use absolutely no trackers, cookies, or tracking pixels. We refuse to use third-party analytics tools (such as Google Analytics). Your browsing on our site is completely invisible and unverifiable by third parties.

## 6. Sovereign Infrastructure, Anti-GAFAM, and the Tor Network

Unlike traditional messaging services, we do not use any SMS verification providers or any content analysis or automated moderation services.

* **Exclusive Network:** Our application's encrypted data only transits through servers located in jurisdictions we have rigorously selected for their respect for privacy: Switzerland, Malaysia, Panama, South Africa, and Iceland.
* **Absolute Independence (Anti-GAFAM):** Your encrypted data does not transit anywhere else. It will never pass through the infrastructure of web giants (Big Tech) or any third-party company subject to the jurisdiction of the United States.
* **Tor Network Accessibility (.onion):** To ensure total anonymity from your very first visit, our website is natively accessible via the Tor network as a hidden service (.onion).
* **Hardware Security Limitation:** The installation tool for our operating system (Web Installer) is intentionally not made available on the Tor network. Because the Tor network struggles with the download of very large files (heavy system images), its use posed a major risk of packet corruption during transfer. Since corrupted data could permanently brick your phone during the flashing process, we have disabled this option as a strict hardware security measure.
* **Payments:** Cryptocurrency payments are processed through our own self-hosted infrastructure (BTCPay Server), without any financial intermediary capable of linking a transaction to a digital identity.

## 7. Legal Basis and Compliance with the new Swiss Law (nFADP / GDPR)

Although we do not collect any personal data related to your communications, European (GDPR) and Swiss legislation require us to specify the legal basis for processing the only data we possess: your name, email address, and country of residence, provided when purchasing ArpokratOS.

* **New Federal Act on Data Protection (nFADP 2023):** Our policy is strictly aligned with the new Federal Act on Data Protection (nFADP), which came into force in Switzerland on September 1, 2023. In accordance with its Article 6 (Principle of Proportionality), which states that the collection and processing of data must be limited to what is strictly necessary and justified, our technical architecture applies absolute data minimization.
* **Performance of a Contract and Legal Obligation:** We process the aforementioned administrative information solely because it is necessary for the performance of the sales contract and to comply with mandatory tax and accounting obligations in Switzerland.
* **No Analysis of Communications:** The European Directive 2002/58/EC (ePrivacy) concerning the retention of metadata (call duration, phone numbers) does not apply to our infrastructure, as we do not possess or generate any of this metadata.
* **Your Rights:** In accordance with the nFADP and the GDPR, regarding your billing data (if you have made a purchase), you have the right to request access to or rectification of this data. However, the erasure of this accounting data can only occur after the legal retention period dictated by the Swiss Code of Obligations (10 years) has expired.
* **Autonomous Exercise of the Right to Erasure:** The user has sovereign control over their data via the "Destruction Code." Activating this tool constitutes the immediate technical exercise of the right to erasure (Art. 17 GDPR), rendering the data mathematically irretrievable. Furthermore, the automatic reboot every 4 hours ensures the periodic erasure of encryption keys from RAM.

## 8. Contacting Us

Since we do not maintain user accounts or any profiling system linking you to our application, any privacy-related inquiries must be submitted in writing to our company's headquarters or through the official encrypted communication channels indicated on our website.

**Arpokrat GmbH** c/o Wadsack AG, Bahnhofstrasse 7, CH-6302, Zug, Switzerland.

## 9. Governing Language (Primacy)

This Privacy Policy may be translated into several languages for the convenience of our users. In the event of any dispute, discrepancy, or conflict of interpretation between the French version and any other translated version of this Policy, the French version shall prevail and be considered the governing version.