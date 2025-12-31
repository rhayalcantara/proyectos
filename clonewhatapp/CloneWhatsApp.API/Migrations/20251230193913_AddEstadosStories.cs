using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CloneWhatsApp.API.Migrations
{
    /// <inheritdoc />
    public partial class AddEstadosStories : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AlterDatabase()
                .Annotation("MySQL:Charset", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "Chats",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false),
                    Tipo = table.Column<int>(type: "int", nullable: false),
                    FechaCreacion = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                    UltimaActividad = table.Column<DateTime>(type: "datetime(6)", nullable: true),
                    NombreGrupo = table.Column<string>(type: "varchar(100)", maxLength: 100, nullable: true),
                    ImagenGrupo = table.Column<string>(type: "varchar(500)", maxLength: 500, nullable: true),
                    DescripcionGrupo = table.Column<string>(type: "varchar(500)", maxLength: 500, nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Chats", x => x.Id);
                })
                .Annotation("MySQL:Charset", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "Usuarios",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false),
                    NumeroTelefono = table.Column<string>(type: "varchar(20)", maxLength: 20, nullable: false),
                    Nombre = table.Column<string>(type: "varchar(100)", maxLength: 100, nullable: false),
                    PasswordHash = table.Column<string>(type: "longtext", nullable: false),
                    FotoPerfil = table.Column<string>(type: "varchar(500)", maxLength: 500, nullable: true),
                    Estado = table.Column<string>(type: "varchar(150)", maxLength: 150, nullable: false),
                    UltimaConexion = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                    EstaEnLinea = table.Column<bool>(type: "tinyint(1)", nullable: false),
                    FechaCreacion = table.Column<DateTime>(type: "datetime(6)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Usuarios", x => x.Id);
                })
                .Annotation("MySQL:Charset", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "ChatParticipantes",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false),
                    ChatId = table.Column<Guid>(type: "char(36)", nullable: false),
                    UsuarioId = table.Column<Guid>(type: "char(36)", nullable: false),
                    Rol = table.Column<int>(type: "int", nullable: false),
                    Silenciado = table.Column<bool>(type: "tinyint(1)", nullable: false),
                    FechaUnion = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                    UltimaLectura = table.Column<DateTime>(type: "datetime(6)", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_ChatParticipantes", x => x.Id);
                    table.ForeignKey(
                        name: "FK_ChatParticipantes_Chats_ChatId",
                        column: x => x.ChatId,
                        principalTable: "Chats",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_ChatParticipantes_Usuarios_UsuarioId",
                        column: x => x.UsuarioId,
                        principalTable: "Usuarios",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                })
                .Annotation("MySQL:Charset", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "Contactos",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false),
                    UsuarioId = table.Column<Guid>(type: "char(36)", nullable: false),
                    ContactoUsuarioId = table.Column<Guid>(type: "char(36)", nullable: false),
                    NombrePersonalizado = table.Column<string>(type: "varchar(100)", maxLength: 100, nullable: true),
                    Bloqueado = table.Column<bool>(type: "tinyint(1)", nullable: false),
                    FechaAgregado = table.Column<DateTime>(type: "datetime(6)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Contactos", x => x.Id);
                    table.ForeignKey(
                        name: "FK_Contactos_Usuarios_ContactoUsuarioId",
                        column: x => x.ContactoUsuarioId,
                        principalTable: "Usuarios",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_Contactos_Usuarios_UsuarioId",
                        column: x => x.UsuarioId,
                        principalTable: "Usuarios",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                })
                .Annotation("MySQL:Charset", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "Estados",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false),
                    UsuarioId = table.Column<Guid>(type: "char(36)", nullable: false),
                    Tipo = table.Column<int>(type: "int", nullable: false),
                    Contenido = table.Column<string>(type: "varchar(1000)", maxLength: 1000, nullable: true),
                    UrlArchivo = table.Column<string>(type: "varchar(500)", maxLength: 500, nullable: true),
                    ColorFondo = table.Column<string>(type: "varchar(20)", maxLength: 20, nullable: true),
                    FechaCreacion = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                    FechaExpiracion = table.Column<DateTime>(type: "datetime(6)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Estados", x => x.Id);
                    table.ForeignKey(
                        name: "FK_Estados_Usuarios_UsuarioId",
                        column: x => x.UsuarioId,
                        principalTable: "Usuarios",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                })
                .Annotation("MySQL:Charset", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "Mensajes",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false),
                    ChatId = table.Column<Guid>(type: "char(36)", nullable: false),
                    RemitenteId = table.Column<Guid>(type: "char(36)", nullable: false),
                    Contenido = table.Column<string>(type: "varchar(4000)", maxLength: 4000, nullable: true),
                    Tipo = table.Column<int>(type: "int", nullable: false),
                    UrlArchivo = table.Column<string>(type: "varchar(500)", maxLength: 500, nullable: true),
                    NombreArchivo = table.Column<string>(type: "varchar(100)", maxLength: 100, nullable: true),
                    TamanoArchivo = table.Column<long>(type: "bigint", nullable: true),
                    MensajeRespondidoId = table.Column<Guid>(type: "char(36)", nullable: true),
                    FechaEnvio = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                    Eliminado = table.Column<bool>(type: "tinyint(1)", nullable: false),
                    EliminadoParaTodos = table.Column<bool>(type: "tinyint(1)", nullable: false),
                    Editado = table.Column<bool>(type: "tinyint(1)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Mensajes", x => x.Id);
                    table.ForeignKey(
                        name: "FK_Mensajes_Chats_ChatId",
                        column: x => x.ChatId,
                        principalTable: "Chats",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_Mensajes_Mensajes_MensajeRespondidoId",
                        column: x => x.MensajeRespondidoId,
                        principalTable: "Mensajes",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_Mensajes_Usuarios_RemitenteId",
                        column: x => x.RemitenteId,
                        principalTable: "Usuarios",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                })
                .Annotation("MySQL:Charset", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "VistasEstados",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false),
                    EstadoId = table.Column<Guid>(type: "char(36)", nullable: false),
                    UsuarioId = table.Column<Guid>(type: "char(36)", nullable: false),
                    FechaVista = table.Column<DateTime>(type: "datetime(6)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_VistasEstados", x => x.Id);
                    table.ForeignKey(
                        name: "FK_VistasEstados_Estados_EstadoId",
                        column: x => x.EstadoId,
                        principalTable: "Estados",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_VistasEstados_Usuarios_UsuarioId",
                        column: x => x.UsuarioId,
                        principalTable: "Usuarios",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                })
                .Annotation("MySQL:Charset", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "EstadosMensaje",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false),
                    MensajeId = table.Column<Guid>(type: "char(36)", nullable: false),
                    UsuarioId = table.Column<Guid>(type: "char(36)", nullable: false),
                    Estado = table.Column<int>(type: "int", nullable: false),
                    FechaEstado = table.Column<DateTime>(type: "datetime(6)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_EstadosMensaje", x => x.Id);
                    table.ForeignKey(
                        name: "FK_EstadosMensaje_Mensajes_MensajeId",
                        column: x => x.MensajeId,
                        principalTable: "Mensajes",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_EstadosMensaje_Usuarios_UsuarioId",
                        column: x => x.UsuarioId,
                        principalTable: "Usuarios",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                })
                .Annotation("MySQL:Charset", "utf8mb4");

            migrationBuilder.CreateIndex(
                name: "IX_ChatParticipantes_ChatId_UsuarioId",
                table: "ChatParticipantes",
                columns: new[] { "ChatId", "UsuarioId" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_ChatParticipantes_UsuarioId",
                table: "ChatParticipantes",
                column: "UsuarioId");

            migrationBuilder.CreateIndex(
                name: "IX_Contactos_ContactoUsuarioId",
                table: "Contactos",
                column: "ContactoUsuarioId");

            migrationBuilder.CreateIndex(
                name: "IX_Contactos_UsuarioId_ContactoUsuarioId",
                table: "Contactos",
                columns: new[] { "UsuarioId", "ContactoUsuarioId" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_Estados_FechaExpiracion",
                table: "Estados",
                column: "FechaExpiracion");

            migrationBuilder.CreateIndex(
                name: "IX_Estados_UsuarioId",
                table: "Estados",
                column: "UsuarioId");

            migrationBuilder.CreateIndex(
                name: "IX_EstadosMensaje_MensajeId_UsuarioId",
                table: "EstadosMensaje",
                columns: new[] { "MensajeId", "UsuarioId" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_EstadosMensaje_UsuarioId",
                table: "EstadosMensaje",
                column: "UsuarioId");

            migrationBuilder.CreateIndex(
                name: "IX_Mensajes_ChatId",
                table: "Mensajes",
                column: "ChatId");

            migrationBuilder.CreateIndex(
                name: "IX_Mensajes_FechaEnvio",
                table: "Mensajes",
                column: "FechaEnvio");

            migrationBuilder.CreateIndex(
                name: "IX_Mensajes_MensajeRespondidoId",
                table: "Mensajes",
                column: "MensajeRespondidoId");

            migrationBuilder.CreateIndex(
                name: "IX_Mensajes_RemitenteId",
                table: "Mensajes",
                column: "RemitenteId");

            migrationBuilder.CreateIndex(
                name: "IX_Usuarios_NumeroTelefono",
                table: "Usuarios",
                column: "NumeroTelefono",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_VistasEstados_EstadoId_UsuarioId",
                table: "VistasEstados",
                columns: new[] { "EstadoId", "UsuarioId" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_VistasEstados_UsuarioId",
                table: "VistasEstados",
                column: "UsuarioId");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "ChatParticipantes");

            migrationBuilder.DropTable(
                name: "Contactos");

            migrationBuilder.DropTable(
                name: "EstadosMensaje");

            migrationBuilder.DropTable(
                name: "VistasEstados");

            migrationBuilder.DropTable(
                name: "Mensajes");

            migrationBuilder.DropTable(
                name: "Estados");

            migrationBuilder.DropTable(
                name: "Chats");

            migrationBuilder.DropTable(
                name: "Usuarios");
        }
    }
}
