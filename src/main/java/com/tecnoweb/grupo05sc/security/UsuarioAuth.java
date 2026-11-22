package com.tecnoweb.grupo05sc.security;

/**
 * Representa la identidad y privilegios del usuario autenticado
 * a través de la dirección del remitente (From:) del correo electrónico.
 */
public final class UsuarioAuth {
    private final long idUsuario;
    private final String nombreCompleto;
    private final String correo;
    private final long idRol;
    private final String rolNombre;
    private final boolean activo;
    private final boolean participaRoundRobin;

    public UsuarioAuth(long idUsuario, String nombreCompleto, String correo, long idRol, String rolNombre, boolean activo, boolean participaRoundRobin) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.idRol = idRol;
        this.rolNombre = rolNombre;
        this.activo = activo;
        this.participaRoundRobin = participaRoundRobin;
    }

    public long getIdUsuario() {
        return idUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public long getIdRol() {
        return idRol;
    }

    public String getRolNombre() {
        return rolNombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public boolean isParticipaRoundRobin() {
        return participaRoundRobin;
    }

    public boolean esAdmin() {
        return idRol == 1 || "Administrador".equalsIgnoreCase(rolNombre);
    }

    public boolean esCoordinador() {
        return idRol == 2 || "Coordinador".equalsIgnoreCase(rolNombre);
    }

    public boolean esVendedor() {
        return idRol == 3 || "Vendedor".equalsIgnoreCase(rolNombre);
    }

    public boolean esPersonal() {
        return esAdmin() || esCoordinador() || esVendedor();
    }

    public String getEmail() {
        return correo;
    }

    @Override
    public String toString() {
        return "UsuarioAuth{" +
                "id=" + idUsuario +
                ", nombre='" + nombreCompleto + '\'' +
                ", correo='" + correo + '\'' +
                ", rol='" + rolNombre + '\'' +
                ", activo=" + activo +
                '}';
    }
}
