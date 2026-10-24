"""Resumen de la cobertura de pruebas de la app (T088).

Lee el report.xml que genera JaCoCo (tarea createDebugUnitTestCoverageReport) y escribe
una tabla en Markdown con el porcentaje total y por capa de la app. En el CI su salida se
agrega a $GITHUB_STEP_SUMMARY, así la tabla aparece en la página del run de GitHub Actions.
Solo informa: no exige un mínimo de cobertura.

Uso: python3 scripts/ci/resumen_cobertura.py [ruta/al/report.xml]
"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPORTE = Path(sys.argv[1] if len(sys.argv) > 1 else 'app/build/reports/coverage/test/debug/report.xml')
PAQUETE_BASE = 'com/urbanblade/mobile/'

# Capas de la app según el paquete. El orden importa: ui/viewmodel va antes que ui/.
CAPAS = [
    ('ui/viewmodel', 'ViewModels (lógica de cada pantalla)'),
    ('ui/', 'Pantallas y componentes (Compose)'),
    ('data/', 'Datos (modelos y repositorios)'),
    ('core/', 'Núcleo (sesión, red, pagos, push…)'),
]
OTROS = 'Otros (MainActivity, Application)'

# Contadores de JaCoCo que se muestran en el total, con su nombre en español.
CONTADORES = [
    ('LINE', 'Líneas'),
    ('BRANCH', 'Ramas (if, when…)'),
    ('METHOD', 'Métodos'),
    ('CLASS', 'Clases'),
]


def contador(elemento, tipo):
    """Regresa (cubiertas, total) del contador `tipo` de un elemento del reporte."""
    c = elemento.find(f"counter[@type='{tipo}']")
    if c is None:
        return 0, 0
    cubiertas = int(c.get('covered'))
    return cubiertas, cubiertas + int(c.get('missed'))


def porcentaje(cubiertas, total):
    return f'{100 * cubiertas / total:.1f} %' if total else '—'


def capa_de(paquete):
    relativo = paquete.removeprefix(PAQUETE_BASE)
    for prefijo, nombre in CAPAS:
        if relativo.startswith(prefijo):
            return nombre
    return OTROS


if not REPORTE.is_file():
    sys.exit(f'No se encontró el reporte de cobertura: {REPORTE}')

raiz = ET.parse(REPORTE).getroot()

print('## Cobertura de pruebas (JaCoCo)\n')
print('| Medida | Cubiertas | Total | Cobertura |')
print('|---|---:|---:|---:|')
for tipo, nombre in CONTADORES:
    cubiertas, total = contador(raiz, tipo)
    print(f'| {nombre} | {cubiertas} | {total} | **{porcentaje(cubiertas, total)}** |')

# Suma las líneas de cada paquete en su capa.
por_capa = {nombre: [0, 0] for _, nombre in CAPAS}
por_capa[OTROS] = [0, 0]
for paquete in raiz.findall('package'):
    cubiertas, total = contador(paquete, 'LINE')
    acumulado = por_capa[capa_de(paquete.get('name'))]
    acumulado[0] += cubiertas
    acumulado[1] += total

print('\n### Líneas cubiertas por capa\n')
print('| Capa | Cubiertas | Total | Cobertura |')
print('|---|---:|---:|---:|')
for nombre, (cubiertas, total) in por_capa.items():
    if total:
        print(f'| {nombre} | {cubiertas} | {total} | {porcentaje(cubiertas, total)} |')

print('\nReporte completo: artefacto `reportes-android`, carpeta `coverage/test/debug/` (abrir `index.html`).')
