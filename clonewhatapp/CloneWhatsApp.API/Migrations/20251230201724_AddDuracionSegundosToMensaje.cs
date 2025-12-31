using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CloneWhatsApp.API.Migrations
{
    /// <inheritdoc />
    public partial class AddDuracionSegundosToMensaje : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<int>(
                name: "DuracionSegundos",
                table: "Mensajes",
                type: "int",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "DuracionSegundos",
                table: "Mensajes");
        }
    }
}
