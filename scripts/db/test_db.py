import pymysql

def main():
    conn = pymysql.connect(
        host='gateway01.ap-northeast-1.prod.aws.tidbcloud.com',
        port=4000,
        user='4H9Mi45RRBnZ8DF.root',
        password='n4iL2gMVZPn2p7ea',
        database='daboyeo',
        ssl={'ssl':{}}
    )
    with conn.cursor() as cursor:
        cursor.execute("SELECT show_date, COUNT(*) FROM showtimes GROUP BY show_date ORDER BY show_date DESC")
        rows = cursor.fetchall()
        if not rows:
            print("NO DATA")
        for r in rows:
            print(f"{r[0]}: {r[1]}")

if __name__ == "__main__":
    main()
