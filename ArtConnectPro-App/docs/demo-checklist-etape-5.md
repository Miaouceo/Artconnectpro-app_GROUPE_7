# Checklist de demonstration

## Avant la demo

- verifier que MySQL est lance
- verifier que la base `artconnect_pro` existe
- executer `01_schema.sql` si besoin
- executer `02_seed.sql`
- executer `03_test_queries.sql`
- verifier que l'application se lance

## Pendant la demo

1. montrer rapidement le schema SQL
2. montrer `DatabaseConfig`, `ConnectionManager` et `ServiceProvider`
3. lancer l'application
4. ouvrir `Artists`
5. ouvrir `Artworks`
6. ouvrir `Galleries` et `Exhibitions`
7. ouvrir `Workshops` et `Community`
8. modifier une donnee en base
9. relancer l'application
10. montrer que la modification apparait

## Captures d'ecran conseillees

- application lancee
- onglet `Artists`
- onglet `Artworks`
- onglet `Galleries` ou `Exhibitions`
- capture MySQL + application avec une meme donnee

## Phrase simple a dire

L'application ArtConnect est maintenant connectee a une vraie base MySQL. Les donnees sont chargees par des DAO JDBC, passent par la couche service, puis sont affichees dans l'interface JavaFX.
