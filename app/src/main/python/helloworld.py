import sqlite3
import os.path

def helloworld(ans):
  try:
    package_dir = os.path.abspath(os.path.dirname(__file__))
    db_dir = os.path.join(package_dir, 'test.db')
    
    connection = sqlite3.connect(db_dir)
    cursor = connection.cursor()
    cursor.execute('SELECT result FROM chem WHERE reagent = ?', (ans,))
    results = cursor.fetchall()
    #print(results[0][0])
    #print(results)
    connection.close()
    #results = "неверное уравнение"
    #print(model_answer(model, filename))
    results = results[0]
    results = results[0]
    return(results)
  except Exception as e:
    #result = "попробуйте еще раз"
    #print("33333")
    #return('Error! Code: {c}, Message, {m}'.format(c = type(e).__name__, m = str(e)))
    return("Попробуйте еще раз")
#print(helloworld('Fe+CuSO4'))
