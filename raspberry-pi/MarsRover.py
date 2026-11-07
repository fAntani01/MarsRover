import time
import socket
import Adafruit_PCA9685
import sys
from math import sin, cos, asin, acos, atan, sqrt, pow, pi, atan2
import os
import subprocess
import signal
from multiprocessing import Process, Queue, Array
from Adafruit_MotorHAT import Adafruit_MotorHAT, Adafruit_DCMotor

camera_x = 0
camera_y = 1

servo_x_min = 220
servo_x_max = 580
servo_y_min = 230
servo_y_max = 550

steer1 = 2
steer2 = 3
steer_min = 280
steer_max = 500

arm0 = 4
arm0_min = 170
arm0_max = 620

arm1 = 5
arm1_min = 200
arm1_max = 650

arm2 = 6
arm2_min = 230
arm2_max = 630





oldDirection = "0"

PORT = 5000
streaming = 1

if streaming:
    subprocess.Popen(["sudo", "sh", "/home/pi/Desktop/MarsRover/stream.sh"])

pwm = Adafruit_PCA9685.PCA9685()

pwm.set_pwm_freq(60)

s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

print('Socket created')

mh = Adafruit_MotorHAT(addr=0x60)

motor1 = mh.getMotor(1)
motor2 = mh.getMotor(2)
em = mh.getMotor(3)
light = mh.getMotor(4)


def close():
    print("Exiting...")
    pwm.set_pwm(camera_x, 0, 390)
    pwm.set_pwm(camera_y, 0, 390)
    pwm.set_pwm(steer1, 0, 395)
    pwm.set_pwm(steer2, 0, 395)
    pwm.set_pwm(camera_x, 4096, 0)
    pwm.set_pwm(camera_y, 4096, 0)
    pwm.set_pwm(steer1, 4096, 0)
    pwm.set_pwm(steer2, 4096, 0)
    pwm.set_pwm(arm0, 4096, 0)
    pwm.set_pwm(arm1, 4096, 0)
    pwm.set_pwm(arm2, 4096, 0)
    motor1.run(Adafruit_MotorHAT.RELEASE)
    motor2.run(Adafruit_MotorHAT.RELEASE)
    em.run(Adafruit_MotorHAT.RELEASE)
    light.run(Adafruit_MotorHAT.RELEASE)
    s.close()


def sigint_handler(signum, frame):
    close()
    sys.exit()


signal.signal(signal.SIGINT, sigint_handler)

pwm.set_pwm(camera_x, 0, 390)
pwm.set_pwm(camera_y, 0, 390)

pwm.set_pwm(steer1, 0, 395)
pwm.set_pwm(steer2, 0, 395)


def moveArm(pwm, dir, P1, init, qu1):

    #print("here")
    pp = [0.0,0.0,0.0]
    pp[0] = P1[0]
    pp[1] = P1[1]
    pp[2] = P1[2]

    L = 11
    Xc = 1
    Yc = 3.3
    r = 1.5
    s = 1.6
    lp = 3.3
    aa = 1
    di = 3.5
    Xq = -1.5
    Yq = 1
    bi = 1
    ll = 4

    Tetaq = -atan(Yq / Xq)
    delta = atan(aa / di)

    arm0 = 4
    arm0_min = 200
    arm0_max = 600

    arm1 = 5
    arm1_min = 199
    arm1_max = 600

    arm2 = 6
    arm2_min = 230
    arm2_max = 630

    while 1:



        exit = 0

        try:
            exit = qu1.get(False)
        except Exception as e:
            pass

        if exit:
            #print ("ahahahahahaha")
            break


        if (dir == "f"):

            pp[0] += 0.1

        elif (dir == "b"):

            pp[0] -= 0.1

        elif (dir == "u"):

            pp[1] += 0.1

        elif (dir == "d"):

            pp[1] -= 0.1

        elif (dir == "r"):

            pp[2] += 1

        elif (dir == "l"):

            pp[2] -= 1




        beta = 2 * asin(sqrt((pow(pp[0], 2) + pow(pp[1], 2)) / (4 * pow(L, 2))))

        alpha = pi / 2 - beta / 2 + atan2(pp[1], pp[0])



        if (not ((25 * pi / 180 <= beta) and (beta <= 155 * pi / 180) and (0 <= alpha) and (alpha <= 75 * pi / 180) and (arm2_min <= pp[2]) and (pp[2] <= arm2_max))):

            if (dir == "f"):

                pp[0] -= 0.1

            elif (dir == "b"):

                pp[0] += 0.1

            elif (dir == "u"):

                pp[1] -= 0.1

            elif (dir == "d"):

                pp[1] += 0.1

            elif (dir == "r"):

                pp[2] -= 1

            elif (dir == "l"):

                pp[2] += 1
            #print("exiting")
            break


        # print(str(P[0]) + ", " + str(P[1]))

        # print(str(beta * 180 / pi) + "      " + str(alpha * 180 / pi))

        phi = pi / 2 - beta

        A = 2 * Xc - 2 * s * cos(phi)
        B = -2 * Yc - 2 * s * sin(phi)
        C = pow(s, 2) + pow(r, 2) + pow(Yc, 2) - pow(Xc, 2) - pow(lp, 2) + 2 * s * Yc * sin(phi)

        Ap = 1 + pow((A / B), 2)

        Bp = -2 * Xc + 2 * A * C / (pow(B, 2))

        Cp = pow(Xc, 2) + pow((C / B), 2) - pow(r, 2)

        Xcos = (-Bp - sqrt(pow(Bp, 2) - 4 * Ap * Cp)) / (2 * Ap)

        Ysin = -(A * Xcos + C) / B

        if (Ysin > 0):
            teta1 = acos((Xc - Xcos) / -r)
        else:
            teta1 = 2 * pi - acos((Xc - Xcos) / -r)

        # print("teta1: " + str(teta1 * 180 / pi))

        PQ = sqrt(pow((di * cos(alpha) - aa * sin(alpha) - Xq), 2) + pow((di * sin(alpha) + aa * cos(alpha) - Yq), 2))
        PO = sqrt(pow(Xq, 2) + pow(Yq, 2))
        gamma = asin(sin(pi - alpha - Tetaq - delta) * PO / PQ)

        teta0 = -(acos((pow(ll, 2) - pow(PQ, 2) - pow(bi, 2)) / (-2 * PQ * bi)) - pi / 2 + gamma - delta) + pi / 2

        # print("teta0: " + str(teta0 * 180 / pi))

        pos0 = int(round(148.148 * teta0 + 200))

        pos1 = int(round(148.148 * teta1 - 78.5))

        pos2 = int(round(pp[2]))

       
        if (pos0 > arm0_min and pos0 < arm0_max):

            pwm.set_pwm(arm0, 0, pos0)

        if (pos1 >= arm1_min and pos1 <= arm1_max):

            pwm.set_pwm(arm1, 0, pos1)

        if (pos2 >= arm2_min and pos2 <= arm2_max):

            pwm.set_pwm(arm2, 0, pos2)

        # print("pos0: " + str(pos0) + "pos1: " + str(pos1))

        if (init):
            break

        time.sleep(0.01)

    #print("antani")
    P[0] = pp[0]
    P[1] = pp[1]
    P[2] = pp[2]

try:

    s.bind(('', PORT))

except socket.error as msg:

    print('Bind failed. Error Code : ' + str(msg[0]) + ' Message ' + msg[1])
    sys.exit()

print('Socket bind complete')

print('Socket now listening on: ' + str(PORT))




q1 = Queue()

P = Array('d', 3)
P[0] = 5.0
P[1] = -0.26
P[2] = 435


p = Process(target=moveArm, args=(pwm, "f", P, 1, q1))
p.start()
p.join()



while 1:

    data, address = s.recvfrom(11)

    data.decode("utf-8")

    #print(str(data))

    if len(data) == 11 and data.startswith(b'b') and data.endswith(b'e'):

        data = data[1:10]

       # print("I received: " + str(data))


        steerf = int(data[0:3])
        steerb = -steerf + 780
        speed = int(data[3:6])
        led = int(data[6:7])
        direction = str(data[7:8].decode("utf-8"))
        armEM = int(data[8:9])

        if direction == "0" and oldDirection != "0":

            try:

                if p.is_alive():
                    q1.put(1)

            except Exception as e:
                pass

        elif direction != "0" and oldDirection == "0":

            p = Process(target=moveArm, args=(pwm, direction, P, 0, q1))
            p.start()

        elif direction != "0" and oldDirection != "0" and direction != oldDirection:

            try:

                if p.is_alive():
                    q1.put(1)

            except Exception as e:
                pass

            p = Process(target=moveArm, args=(pwm, direction, P, 0, q1))
            p.start()

        oldDirection = direction

        # print("x: " + str(x))
        # print("y:" + str(y))
        # print("steer: " + str(steerf))
        # print("speed: " + str(speed))
        # print("em: " + str(armEM))
        # print("arm: " + direction)
        # print("LED: " + str(led))

        if (steer_min <= steerf and steerf <= steer_max):
            pwm.set_pwm(steer1, 0, steerf)
            pwm.set_pwm(steer2, 0, steerb)

        if (0 <= speed and speed <= 130):
            motor1.run(Adafruit_MotorHAT.BACKWARD)
            motor2.run(Adafruit_MotorHAT.BACKWARD)
            motor1.setSpeed(speed)
            motor2.setSpeed(speed)

        if (131 <= speed and speed <= 261):
            motor1.run(Adafruit_MotorHAT.FORWARD)
            motor2.run(Adafruit_MotorHAT.FORWARD)
            motor1.setSpeed(speed - 131)
            motor2.setSpeed(speed - 131)

        if (armEM == 1):
            em.run(Adafruit_MotorHAT.FORWARD)
            em.setSpeed(255)

        elif (armEM == 0):
            em.setSpeed(0)

        if (led == 1):
            light.run(Adafruit_MotorHAT.FORWARD)
            light.setSpeed(200)

        elif (led == 0):
            light.setSpeed(0)

    elif len(data) == 11 and data.startswith(b'c') and data.endswith(b'f'):

        data = data[1:10]

        x = int(data[0:3])
        y = int(data[3:6])

        if servo_x_min <= x and x <= servo_x_max:
            pwm.set_pwm(camera_x, 0, x)

        if servo_y_min <= y and y <= servo_y_max:
            pwm.set_pwm(camera_y, 0, y)

    else:

        print("data error: " + str(data))

close()

s.close()
