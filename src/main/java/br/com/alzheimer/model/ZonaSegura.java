package br.com.alzheimer.model;

public class ZonaSegura {
    private int id;
    private int pacienteId;
    private String nome;
    private double latitudeCentro;
    private double longitudeCentro;
    private double raioMetros;

    public ZonaSegura(int id, int pacienteId, String nome, double latitudeCentro, double longitudeCentro, double raioMetros) {
        if (raioMetros <= 0) {
            throw new IllegalArgumentException("O raio da zona segura deve ser maior que zero.");
        }
        this.id = id;
        this.pacienteId = pacienteId;
        this.nome = nome;
        this.latitudeCentro = latitudeCentro;
        this.longitudeCentro = longitudeCentro;
        this.raioMetros = raioMetros;
    }

    public boolean estaDentro(double latAtual, double lonAtual) {
        return calcularDistanciaMetros(latitudeCentro, longitudeCentro, latAtual, lonAtual) <= raioMetros;
    }

    public double calcularDistanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public int getId() { return id; }
    public int getPacienteId() { return pacienteId; }
    public String getNome() { return nome; }
    public double getLatitudeCentro() { return latitudeCentro; }
    public double getLongitudeCentro() { return longitudeCentro; }
    public double getRaioMetros() { return raioMetros; }
}
