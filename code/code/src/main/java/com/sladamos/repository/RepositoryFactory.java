package com.sladamos.repository;

public class RepositoryFactory {

    private final String dbProfile;

    public RepositoryFactory(String dbProfile) {
        this.dbProfile = dbProfile;
    }

    public BenchmarkRepository getBenchmarkRepository(String repositoryName) {
        return switch (repositoryName) {
            case "hibernate" -> new HibernateRepository(dbProfile);
            case "eclipselink" -> new EclipseLinkRepository(dbProfile);
            case "datanucleus" -> new DataNucleusRepository(dbProfile);
            default -> throw new IllegalArgumentException("Nieznany ORM: " + repositoryName);
        };
    }
}
