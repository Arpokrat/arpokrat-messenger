# Arpokrat

> **Code, not promises. Chat and transact on your own terms.**

Security isn't a marketing buzzword; it's a constant, ongoing process. Arpokrat doesn't claim to be "ultra-secure" or unbreakable—nothing is. It is simply an open-source tool built on sound cryptography, designed to let you communicate and transact without relying on trusted third parties.

We write code to keep conversations private and assets personal. That's it.

## The Tools

* **Anonymous Routing:** Built on the SimpleX protocol. There are no user IDs, no phone numbers, and no central servers to leak metadata. It just routes encrypted text from node to node.
* **Local Cryptography:** A non-custodial multi-chain wallet built directly into the client. It handles UTXOs, constructs ABIs, and signs transactions locally for Bitcoin, EVM chains (Ethereum, Polygon), Solana, and Tron. Your device, your seed phrase.
* **Peer-to-Peer Economy:** Send a transaction or request funds directly in a chat. It's just signed data broadcasted to the network. No custodians, no middlemen.

## Don't trust. Verify.

This codebase is open. It was built by Arpokrat GmbH in Zug because we wanted a messaging client that actually respects its users. But don't take our word for it—read the source, audit the math, and compile it yourself. 

To build the Android/Multiplatform application, navigate to `apps/multiplatform` and follow the standard Gradle build instructions.

---

## Acknowledgments

Arpokrat Messenger is proudly built as a fork of [SimpleX Chat](https://github.com/simplex-chat/simplex-chat). 

We want to extend our deepest gratitude to the SimpleX team for their remarkable and groundbreaking work on the core routing protocol. Their uncompromising dedication to metadata privacy and their brilliant engineering laid the foundation for this project. Without them, Arpokrat would not exist. 

## License

This software is licensed under the GNU Affero General Public License version 3 (AGPLv3). See the [LICENSE](./LICENSE) file for details.