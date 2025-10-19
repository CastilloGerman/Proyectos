# -*- mode: python ; coding: utf-8 -*-


a = Analysis(
    ['bdpeliculas.py'],
    pathex=['.'],
    binaries=[],
    datas=[
        ('CatalogoPeliculas/database/*.db', 'CatalogoPeliculas/database'),
        ('CatalogoPeliculas/img/*.ico', 'CatalogoPeliculas/img'),
    ],
    hiddenimports=[],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    noarchive=False,
    optimize=0,
)
pyz = PYZ(a.pure)

exe = EXE(
    pyz,
    a.scripts,
    [],
    exclude_binaries=True,
    name='CatalogoPeliculas',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    console=False,
    icon='CatalogoPeliculas/img/catalogo.ico',
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
)
coll = COLLECT(
    exe,
    a.binaries,
    a.datas,
    strip=False,
    upx=True,
    upx_exclude=[],
    name='CatalogoPeliculas',
)
