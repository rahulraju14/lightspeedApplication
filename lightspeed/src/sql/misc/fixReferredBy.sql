
 select * from user where referred_by like 'fs%';
/*
-- update user set referred_by = 'fs-dh' where id > 712 and
select * from user where -- id > 712 and
email_address in (
'PaulRoseJr@1330Productions.com',
'ramdeenoo@rogers.com',
'ramdeenoo@gmail.com',
'sparksbc1@gmail.com',
'Batman@azteca.net',
'stijnkl@me.com',
'7cohen7@gmail.com',
'Tanemdavidson@gmail.com',
'info@overwaterexpeditions.org',
'karita.fleming@gmail.com',
'whiteratxo@yahoo.com',
'dianesavage92@yahoo.com',
'webpro1@optimum.net',
'ocor@gmx.de',
'jandrucci@gmail.com',
'notperfectproductions@gmail.com',
'stalentino@gmail.com',
'lance@csc.ca',
'depedrodigital@gmail.com',
'gpsfilmproduction@gmail.com',
'jeffrey.poehlmann@gmail.com',
'pittmanent@gmail.com',
'james@movingmetaphor.com',
'wmurph11@yahoo.com',
'sushilastroworld@gmail.com',
'rudacanostalgico@hotmail.com',
'ikilledaghost@ymail.com'
)
;
/* substring(description,locate('ip=',description)+3) as IP */
/*
select username

	from `dba1`.`event` where type='login_ok' and start_time > '2012-08-02 12:00:00'
  and description regexp '.*IP=.*'
  and substring(description,locate('ip=',description)+3) in
(
'1.38.16.153',
'117.199.98.131',
'128.196.20.165',
'173.167.98.162',
'173.20.93.231',
'173.56.48.67',
'186.14.192.169',
'198.53.116.204',
'199.27.175.2',
'217.255.109.53',
'24.248.58.195',
'41.31.69.41',
'50.9.61.245',
'64.134.179.74',
'64.61.173.195',
'66.177.160.225',
'66.233.31.198',
'67.173.25.169',
'67.204.0.139',
'67.70.108.217',
'68.10.170.130',
'68.198.8.169',
'68.33.43.150',
'69.115.58.109',
'69.89.205.214',
'71.103.149.236',
'74.195.90.158',
'75.65.222.97',
'75.70.12.43',
'76.195.144.221',
'76.91.240.52',
'82.26.33.169',
'86.22.126.67',
'93.131.22.62',
'94.229.59.81',
'98.154.51.47',
'98.250.182.97',
'210.49.83.178', 
'178.116.195.89' 
 )
group by username
order by start_time;

/*
select `Start_Time`, `Username`,
        substring(description,locate('ip=',description)+3) as IP
	from `dba1`.`event` where type='login_ok' and start_time > '2012-08-02 12:00:00'
  and description regexp '.*IP=.*'
  group by username
  order by ip;
 /* */
 /*
 select * from `dba1`.`event` where description like '%%'
 /* */
 