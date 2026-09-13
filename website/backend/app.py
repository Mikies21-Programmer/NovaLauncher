from flask import Flask, render_template

app = Flask(__name__)

@app.route('/')
@app.route('/home')
def home():
    return render_template('home.html')

@app.route('/features')
def features():
    return render_template('features.html')

@app.route('/download')
def download():
    return render_template('download.html')

@app.route('/politicas_de_privacidad')
def privacy():
    return render_template('politicas_de_privacidad.html')

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5000, debug=True)
