package com.mycompany.examgenai_backend.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChapterExtractorTest {

    private final ChapterExtractor chapterExtractor = new ChapterExtractor();

    @Test
    void extraitPlusieursChapitresAvecLeurContenu() {
        String texte = """
                Introduction générale au cours.

                Chapitre 1: Les bases de Java
                Java est un langage orienté objet.
                Il a été créé par Sun Microsystems.

                Chapitre 2: La programmation orientée objet
                Les classes et les objets sont les briques de base.
                """;

        List<ChapterExtractor.ExtractedChapter> chapitres = chapterExtractor.extract(texte);

        assertEquals(2, chapitres.size());

        assertEquals("Les bases de Java", chapitres.get(0).title());
        assertTrue(chapitres.get(0).content().contains("Sun Microsystems"));
        assertFalse(chapitres.get(0).content().contains("Chapitre 2"));

        assertEquals("La programmation orientée objet", chapitres.get(1).title());
        assertTrue(chapitres.get(1).content().contains("briques de base"));
    }

    @Test
    void creeUnChapitreUniqueQuandAucunTitreDetecte() {
        String texte = "Ce document ne contient aucun titre de chapitre, juste du texte continu.";

        List<ChapterExtractor.ExtractedChapter> chapitres = chapterExtractor.extract(texte);

        assertEquals(1, chapitres.size());
        assertEquals("Cours complet", chapitres.get(0).title());
        assertEquals(texte, chapitres.get(0).content());
    }

    @Test
    void gereUnTexteVideOuNullSansPlanter() {
        List<ChapterExtractor.ExtractedChapter> depuisTexteVide = chapterExtractor.extract("   ");
        assertEquals(1, depuisTexteVide.size());
        assertFalse(depuisTexteVide.get(0).content().isBlank());

        List<ChapterExtractor.ExtractedChapter> depuisNull = chapterExtractor.extract(null);
        assertEquals(1, depuisNull.size());
        assertFalse(depuisNull.get(0).content().isBlank());
    }

    @Test
    void detecteLesVariantesDeCasseEtLesAbreviations() {
        String texte = """
                CHAPITRE 1 - Les fondations
                Contenu du premier chapitre.

                Chap 2 : La suite
                Contenu du second chapitre.
                """;

        List<ChapterExtractor.ExtractedChapter> chapitres = chapterExtractor.extract(texte);

        assertEquals(2, chapitres.size());
        assertEquals("Les fondations", chapitres.get(0).title());
        assertEquals("La suite", chapitres.get(1).title());
    }

    @Test
    void chapitreSansContenuRecoitUnMessageParDefaut() {
        String texte = """
                Chapitre 1: Premier
                Chapitre 2: Deuxième
                Un peu de texte ici.
                """;

        List<ChapterExtractor.ExtractedChapter> chapitres = chapterExtractor.extract(texte);

        assertEquals(2, chapitres.size());
        assertFalse(chapitres.get(0).content().isBlank());
        assertTrue(chapitres.get(1).content().contains("Un peu de texte ici"));
    }
}
