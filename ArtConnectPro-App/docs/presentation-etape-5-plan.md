# Plan de presentation - ArtConnect Pro

## Slide 1 - Titre

- ArtConnect Pro
- Base de donnees et application Java
- Projet BDD / JDBC / JavaFX

## Slide 2 - Objectif du projet

- creer une base de donnees pour une communaute artistique
- connecter cette base a une application Java
- afficher et manipuler des donnees persistantes

## Slide 3 - Conception de la base

- MCD
- MLD
- tables principales
- relations entre artistes, oeuvres, galeries, ateliers et membres

## Slide 4 - Normalisation et contraintes

- 1FN, 2FN, 3FN
- cles primaires et etrangeres
- contraintes metier

## Slide 5 - Architecture de l'application

- entites
- DAO
- services
- interface JavaFX
- JDBC

## Slide 6 - DAO et services JDBC

- `JdbcArtistDao`
- `JdbcArtworkDao`
- `JdbcGalleryDao`
- `JdbcWorkshopDao`
- `JdbcCommunityMemberDao`

## Slide 7 - Connexion a la base

- `DatabaseConfig`
- `ConnectionManager`
- `ServiceProvider`

## Slide 8 - Donnees d'exemple et tests

- `01_schema.sql`
- `02_seed.sql`
- `03_test_queries.sql`

## Slide 9 - Demonstration

- lancement de l'application
- artistes et oeuvres
- galeries et expositions
- ateliers et membres
- preuve qu'une modification en base apparait dans l'application

## Slide 10 - Difficultes et solutions

- absence d'identifiants dans les classes Java
- reconstruction des relations avec JDBC
- gestion des contraintes SQL
- liaison correcte entre base et application

## Slide 11 - Conclusion

- base relationnelle fonctionnelle
- application connectee a MySQL
- projet finalise et presentable
