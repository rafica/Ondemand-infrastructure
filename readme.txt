Control.java has the main program. 
step 1: When this program runs, username is asked as input. username is matched with the elastic ip in the dictionary and the values of ami and 
elastic ip is passed as parameters to Ondemand_core_function .


step 2:cloud watch checks every ten minutes for cpu utlization. if its idle, the instance is terminated and ami is created. 
snapshot of the volume is also taken. value of ami is returned to the main program control.java

step 3:the next day , step 1 to 2 is repeated