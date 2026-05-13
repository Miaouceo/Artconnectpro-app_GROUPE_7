package com.project.artconnect.service.impl;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.service.ArtistService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class JdbcArtistService implements ArtistService {
    private final ArtistDao artistDao;

    public JdbcArtistService(ArtistDao artistDao) {
        this.artistDao = artistDao;
    }

    @Override
    public List<Artist> getAllArtists() {
        return artistDao.findAll();
    }

    @Override
    public Optional<Artist> getArtistByName(String name) {
        return artistDao.findAll().stream()
                .filter(artist -> artist.getName() != null && artist.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public void createArtist(Artist artist) {
        artistDao.save(artist);
    }

    @Override
    public void updateArtist(Artist artist) {
        artistDao.update(artist);
    }

    @Override
    public void deleteArtist(String name) {
        artistDao.delete(name);
    }

    @Override
    public List<Discipline> getAllDisciplines() {
        Map<String, Discipline> disciplines = new LinkedHashMap<>();
        for (Artist artist : artistDao.findAll()) {
            for (Discipline discipline : artist.getDisciplines()) {
                if (discipline != null && discipline.getName() != null && !discipline.getName().isBlank()) {
                    disciplines.putIfAbsent(discipline.getName().toLowerCase(), discipline);
                }
            }
        }
        return new ArrayList<>(disciplines.values());
    }

    @Override
    public List<Artist> searchArtists(String query, String disciplineName, String city) {
        return artistDao.findAll().stream()
                .filter(artist -> query == null || query.isBlank()
                        || (artist.getName() != null && artist.getName().toLowerCase().contains(query.toLowerCase())))
                .filter(artist -> city == null || city.isBlank()
                        || (artist.getCity() != null && artist.getCity().equalsIgnoreCase(city)))
                .filter(artist -> disciplineName == null || disciplineName.isBlank()
                        || artist.getDisciplines().stream()
                                .anyMatch(discipline -> disciplineName.equalsIgnoreCase(discipline.getName())))
                .collect(Collectors.toList());
    }
}
