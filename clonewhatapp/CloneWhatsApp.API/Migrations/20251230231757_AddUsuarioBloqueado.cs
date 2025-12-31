using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CloneWhatsApp.API.Migrations
{
    /// <inheritdoc />
    public partial class AddUsuarioBloqueado : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "UsuariosBloqueados",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false),
                    UsuarioId = table.Column<Guid>(type: "char(36)", nullable: false),
                    BloqueadoId = table.Column<Guid>(type: "char(36)", nullable: false),
                    FechaBloqueo = table.Column<DateTime>(type: "datetime(6)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_UsuariosBloqueados", x => x.Id);
                    table.ForeignKey(
                        name: "FK_UsuariosBloqueados_Usuarios_BloqueadoId",
                        column: x => x.BloqueadoId,
                        principalTable: "Usuarios",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_UsuariosBloqueados_Usuarios_UsuarioId",
                        column: x => x.UsuarioId,
                        principalTable: "Usuarios",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                })
                .Annotation("MySQL:Charset", "utf8mb4");

            migrationBuilder.CreateIndex(
                name: "IX_UsuariosBloqueados_BloqueadoId",
                table: "UsuariosBloqueados",
                column: "BloqueadoId");

            migrationBuilder.CreateIndex(
                name: "IX_UsuariosBloqueados_UsuarioId_BloqueadoId",
                table: "UsuariosBloqueados",
                columns: new[] { "UsuarioId", "BloqueadoId" },
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "UsuariosBloqueados");
        }
    }
}
