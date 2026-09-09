// @ts-check
const eslint = require("@eslint/js");
const tseslint = require("typescript-eslint");
const angular = require("angular-eslint");

module.exports = tseslint.config(
  {
    files: ["**/*.ts"],
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.recommended,
      ...angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      // El codigo heredado (Angular 7) aun usa inyeccion por constructor, `any`
      // y componentes basados en NgModule. Se dejan como avisos para no bloquear
      // la construccion; endurecerlos es trabajo de la practica de calidad.
      "@angular-eslint/prefer-inject": "warn",
      "@angular-eslint/prefer-standalone": "warn",
      "@angular-eslint/no-empty-lifecycle-method": "warn",
      "@typescript-eslint/no-explicit-any": "warn",
      "@typescript-eslint/no-unused-vars": [
        "warn",
        { argsIgnorePattern: "^_", varsIgnorePattern: "^_" },
      ],
      "@typescript-eslint/no-wrapper-object-types": "warn",
      "@angular-eslint/directive-selector": [
        "error",
        { type: "attribute", prefix: "app", style: "camelCase" },
      ],
      "@angular-eslint/component-selector": [
        "error",
        { type: "element", prefix: "app", style: "kebab-case" },
      ],
    },
  },
  {
    files: ["**/*.html"],
    extends: [
      ...angular.configs.templateRecommended,
      // templateAccessibility queda pendiente para la practica de calidad:
      // hoy reporta ~90 avisos en las plantillas heredadas.
    ],
    rules: {
      // Migrar *ngIf/*ngFor al nuevo control flow (@if/@for) y normalizar ===
      // son cambios de codigo: quedan como avisos para una practica posterior.
      "@angular-eslint/template/prefer-control-flow": "warn",
      "@angular-eslint/template/eqeqeq": "warn",
    },
  }
);
