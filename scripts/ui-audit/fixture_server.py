"""API sintética local para revisión visual. No conecta con Laravel ni Stripe.

python scripts/ui-audit/fixture_server.py
Build: -PUI_AUDIT=true -PDEBUG_API_BASE_URL=http://10.0.2.2:18765/api/v1/
Control local: POST /_qa/control con {"status": 503}.
"""
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

USER = {"id": "qa-client", "name": "Cliente de prueba", "email": "cliente@example.test",
        "roles": ["cliente"], "profile_complete": True, "client_id": "qa-client-profile",
        "created_at": "2026-01-01", "email_verified_at": "2026-01-01"}
STATE = {"favorite": None, "status": 200, "slots_status": 200}
SERVICES = [{"id": "qa-service", "nombre": "Corte de prueba", "precio": 100,
             "duracion_min": 30, "descripcion": "Servicio sintético para revisar la interfaz."}]

def member_pdf():
    """PDF mínimo, válido y sintético; no contiene un miembro real."""
    stream = b'BT /F1 18 Tf 40 120 Td (UrbanBlade - TARJETA QA SIN VALIDEZ) Tj ET'
    objects = [b'<< /Type /Catalog /Pages 2 0 R >>',
               b'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
               b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 500 200] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
               b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
               b'<< /Length '+str(len(stream)).encode()+b' >>\nstream\n'+stream+b'\nendstream']
    data = b'%PDF-1.4\n'
    offsets = [0]
    for i, obj in enumerate(objects, 1):
        offsets.append(len(data))
        data += str(i).encode()+b' 0 obj\n'+obj+b'\nendobj\n'
    start = len(data)
    data += b'xref\n0 6\n0000000000 65535 f \n'
    data += b''.join(f'{offset:010d} 00000 n \n'.encode() for offset in offsets[1:])
    return data+b'trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n'+str(start).encode()+b'\n%%EOF\n'

class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_): pass  # No registrar cabeceras, cuerpos ni credenciales.
    def respond(self, payload, status=200):
        data = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        path = urlparse(self.path).path.removeprefix("/api/v1/")
        code = STATE["slots_status"] if path == "availability/slots" else STATE["status"]
        if code != 200: return self.respond({"message": "Respuesta de prueba."}, code)
        if path == "dashboard/membership/card":
            data = member_pdf()
            self.send_response(200)
            self.send_header("Content-Type", "application/pdf")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
            return
        payloads = {
            "auth/me": {"user": USER}, "profile": {"user": USER},
            "services": {"data": SERVICES},
            "barbers": {"data": [{"id": "qa-barber", "slug": "barbero-prueba", "user": {"name": "Barbero de prueba"}, "es_favorito": STATE["favorite"] == "qa-barber"}]},
            "availability/slots": {"slots": [{"time": "12:00", "label": "12:00", "end_time": "12:30", "end_label": "12:30"}]},
            "dashboard": {"role": "cliente", "data": {"loyalty": {"puntos": 0, "nivel": "bronce", "nivel_label": "Bronce"}}},
            "memberships/mine": {"data": None}, "memberships/plans": {"data": []},
            "packages": {"data": []}, "packages/catalog": {"data": [{"id": "qa-pack", "nombre": "Paquete de prueba", "cantidad_usos": 3, "precio": 250, "vigencia_dias": 90}]},
            "gift-cards/mine": {"data": []}, "referrals/mine": {"data": {"code": "QA-REFERIDO", "referidos": []}},
            "payments/cards": {"data": []}, "payments/transfer-info": {"data": {}},
            "notifications/preferences": {"data": {"in_app": True, "email": True}},
            "appointments": {"data": [], "stats": {}}, "notifications": {"data": []},
            "store/products": {"data": []}, "no-show-fees": {"data": [], "adeudo_total": 0},
        }
        self.respond(payloads.get(path, {"data": []}))

    def do_POST(self):
        path = urlparse(self.path).path.removeprefix("/api/v1/")
        data = json.loads(self.rfile.read(int(self.headers.get("Content-Length", 0))) or b"{}")
        if self.path == "/_qa/control":
            STATE.update({k: v for k, v in data.items() if k in STATE})
            roles = data.get("roles")
            if isinstance(roles, list) and roles and all(role in ("cliente", "administrador", "recepcionista", "barbero", "ingeniero") for role in roles):
                USER["roles"] = roles
            return self.respond({"ok": True})
        if STATE["status"] != 200: return self.respond({"message": "Respuesta de prueba."}, STATE["status"])
        if path in ("auth/login", "auth/register", "auth/google/token"):
            return self.respond({"token": "qa-local-token", "user": USER})
        if path in ("packages/stripe-intent", "gift-cards/stripe-intent"):
            return self.respond({"message": "Servidor sintético: no se crean cobros."}, 422)
        return self.respond({"message": "Acción de prueba completada."})

    def do_PUT(self):
        data = json.loads(self.rfile.read(int(self.headers.get("Content-Length", 0))) or b"{}")
        if self.path.endswith("profile/favorite-barber"):
            STATE["favorite"] = data.get("barber_id")
            return self.respond({"barbero_favorito_id": STATE["favorite"]})
        self.respond({"message": "Acción de prueba completada.", "user": USER})

if __name__ == "__main__":
    print("QA local en 127.0.0.1:18765; datos sintéticos; sin cobros.", flush=True)
    ThreadingHTTPServer(("127.0.0.1", 18765), Handler).serve_forever()
