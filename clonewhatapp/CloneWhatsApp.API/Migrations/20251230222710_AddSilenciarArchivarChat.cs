using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CloneWhatsApp.API.Migrations
{
    /// <inheritdoc />
    public partial class AddSilenciarArchivarChat : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<bool>(
                name: "Archivado",
                table: "ChatParticipantes",
                type: "tinyint(1)",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<DateTime>(
                name: "SilenciadoHasta",
                table: "ChatParticipantes",
                type: "datetime(6)",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "Archivado",
                table: "ChatParticipantes");

            migrationBuilder.DropColumn(
                name: "SilenciadoHasta",
                table: "ChatParticipantes");
        }
    }
}
