"""
Sistema de Gestión de Presupuestos
Aplicación para gestionar clientes, materiales y presupuestos
"""

from .clientes import cliente_manager
from .materiales import material_manager
from .presupuestos import presupuesto_manager
from .facturas import factura_manager
from .utils import db
from .pdf_generator import pdf_generator
from .email_sender import email_sender

__version__ = "1.0.0"
__author__ = "Sistema de Presupuestos"

__all__ = [
    'cliente_manager',
    'material_manager', 
    'presupuesto_manager',
    'factura_manager',
    'db',
    'pdf_generator',
    'email_sender'
]
