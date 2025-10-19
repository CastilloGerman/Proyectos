from flask import Flask, jsonify
import requests

app = Flask(__name__)
city = input("Introduce la ciudad: ")

@app.route('/api/clima/<city>', methods=['GET'])
def obtener_clima(city):
    api_key = "868f194885197ddf960b3282afe77975"  # Tu API key
    url = "https://api.openweathermap.org/data/2.5/weather?q={}&appid=868f194885197ddf960b3282afe77975&units=metric&lang=es".format(city)
    response = requests.get(url)
    if response.status_code == 200:
        data = response.json()
        resultado = {
            "city": data["name"],
            "temperatura": data["main"]["temp"],
            "descripcion": data["weather"][0]["description"]
        }
        print("Ciudad: {}, Temperatura: {}°C, Descripción: {}".format(
            data["name"], data["main"]["temp"], data["weather"][0]["description"]
        ))
        return jsonify(resultado)
    else:
        return jsonify({"error": "Ciudad no encontrada"}), 404

if __name__ == '__main__':
    app.run(debug=True)