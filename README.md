# Roucariol and Carvalho
#### (Distributed Mutual Exclusion Algorithm)

[Roucariol and Carvalho](https://fr.wikipedia.org/wiki/Algorithme_Carvalho_et_Roucairol) is an evolution of the [*Ricart and Agrawala*](https://en.wikipedia.org/wiki/Ricart%E2%80%93Agrawala_algorithm) mutual exclusion algorithm. Roucariol considers that, if a client have already given permission for you to run, it is not necessary to ask permission again. Therefore, a system to track this *allowance* is used.

## General Information

- **Java Socket** was used to establish the communications between the clients.
- We simulate a **Printer** as the shared resource that can only be accessed by one client at a time.
- We used *Netbeans IDE* for development, therefore the project follows Netbeans folder and package structure.
