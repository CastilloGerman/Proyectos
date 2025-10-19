import requests

# URL de la API pública
url = "https://api.chucknorris.io/jokes/random"

# Hacemos la petición GET
response = requests.get(url)

# Convertimos la respuesta a JSON
data = response.json()

# Mostramos el chiste
print("Chiste:", data["value"])
