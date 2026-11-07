Imports System.Net
Imports System.Net.Sockets
Imports System.Text
Imports SharpDX.DirectInput

Class Form1


    Public RoverIP As String
    Public RoverPort As Integer
    Public AndroidPort As Integer
    Public StreamingPort As Integer
    Public Streaming As Integer
    Public time As Integer
    Public Music As Boolean

    Dim joystick As Joystick

    Dim udpClient As UdpClient

    Dim joystickState As JoystickState

    Dim speed As Integer

    Dim prevJ As Boolean

    Dim led As Boolean

    Dim prevJ1 As Boolean

    Dim em As Boolean

    Dim receivingUdpClient As UdpClient
    Dim RemoteIPEndPoint As IPEndPoint


    Dim t As Integer





    Private Sub Form1_Load(sender As Object, e As EventArgs) Handles MyBase.Load

        Dim directInput As DirectInput = New DirectInput

        Dim joystickGuid As Guid = Guid.Empty

        t = time

        For Each deviceInstance As DeviceInstance In directInput.GetDevices(DeviceType.Gamepad, DeviceEnumerationFlags.AllDevices)

            joystickGuid = deviceInstance.InstanceGuid

        Next

        If (joystickGuid = Guid.Empty) Then

            For Each deviceInstance As DeviceInstance In directInput.GetDevices(DeviceType.Joystick, DeviceEnumerationFlags.AllDevices)

                joystickGuid = deviceInstance.InstanceGuid

            Next

        End If


        If (joystickGuid = Guid.Empty) Then

            MsgBox("No devices found")
            End

        End If


        joystick = New Joystick(directInput, joystickGuid)


        joystick.Properties.BufferSize = 128

        joystick.Acquire()

        Dim startInfo As New ProcessStartInfo("C:\gstreamer\1.0\x86_64\bin\gst-launch-1.0.exe")

        If Streaming = 1 Then

            Dim arg As String = "udpsrc port=" & StreamingPort.ToString & " caps=""application/x-rtp, media=video, clock-rate=90000, encoding-name=H264, payload=96"" ! rtpjitterbuffer ! rtph264depay ! avdec_h264 ! queue ! videobox border-alpha=0 top=-180 left=-1280 ! videomixer name=mix ! autovideosink sync=false ksvideosrc ! decodebin ! queue ! mix."

            startInfo.Arguments = arg

            Process.Start(startInfo)

        ElseIf Streaming = 2 Then

            Dim arg As String = "udpsrc port=" & StreamingPort.ToString() & " caps=""application/x-rtp, media=video, clock-rate=90000, encoding-name=H264, payload=96"" ! rtpjitterbuffer ! rtph264depay ! avdec_h264 ! autovideosink sync=false"

            startInfo.Arguments = arg

            Process.Start(startInfo)

            arg = "ksvideosrc ! autovideosink sync=false"

            startInfo.Arguments = arg

            Process.Start(startInfo)

        End If

        udpClient = New UdpClient(RoverIP, RoverPort)

        udpClient.EnableBroadcast = True

        receivingUdpClient = New UdpClient(AndroidPort)

        RemoteIPEndPoint = New IPEndPoint(IPAddress.Any, AndroidPort)

        receivingUdpClient.BeginReceive(AddressOf UDPRecv, Nothing)

        joystickState = New JoystickState




    End Sub

    Private Sub Button1_Click(sender As Object, e As EventArgs) Handles Button1.Click

        If (Button1.Text = "Start") Then

            startRover()

        ElseIf Button1.Text = "Stop" Then

            stopRover()

        End If

    End Sub

    Private Sub Timer1_Tick(sender As Object, e As EventArgs) Handles Timer1.Tick


        joystick.GetCurrentState(joystickState)

        Dim steer As Integer = 0.003357 * joystickState.X + 280

        'Dim cameraX As Integer = -(360 / 65535) * joystickState.Z + 580

        'Dim cameraY As Integer = 0.004883 * joystickState.RotationZ + 230

        Dim a As Boolean = joystickState.Buttons(1)

        If (prevJ = False And a = True) Then

            led = Not led

        End If

        prevJ = a

        Dim intLed As Integer = led

        intLed = -intLed


        Dim b As Boolean = joystickState.Buttons(3)

        If (prevJ1 = False And b = True) Then

            em = Not em

        End If

        prevJ1 = b

        Dim intEm As Integer = em


        intEm = -intEm



        If (joystickState.RotationY = 0 And joystickState.RotationX <> 0) Then

            speed = (130 / 65535) * joystickState.RotationX + 131


        ElseIf (joystickState.RotationY <> 0 And joystickState.RotationX = 0) Then

            speed = (130 / 65535) * joystickState.RotationY

        ElseIf (joystickState.RotationY = 0 And joystickState.RotationX = 0) Then

            speed = 0

        End If




        Dim armDir As String = "0"
        Dim count As Integer = 0

        If (joystickState.PointOfViewControllers(0) = 0) Then

            armDir = "f"
            count += 1

        End If

        If (joystickState.PointOfViewControllers(0) = 18000) Then

            armDir = "b"
            count += 1

        End If

        If (joystickState.Buttons(5)) Then

            armDir = "u"
            count += 1

        End If

        If (joystickState.Buttons(4)) Then

            armDir = "d"
            count += 1

        End If

        If (joystickState.PointOfViewControllers(0) = 9000) Then

            armDir = "r"
            count += 1

        End If

        If (joystickState.PointOfViewControllers(0) = 27000) Then

            armDir = "l"
            count += 1

        End If

        If (joystickState.PointOfViewControllers(0) <> 0 And joystickState.PointOfViewControllers(0) <> 9000 And joystickState.PointOfViewControllers(0) <> 18000 And joystickState.PointOfViewControllers(0) <> 27000 And joystickState.PointOfViewControllers(0) <> -1) Then

            count += 1

        End If

        If (count > 1) Then

            armDir = "0"

        End If

        count = 0


        If speed <= 130 Then

            Label1.Text = "Speed: " & (speed / 8.666).ToString("00") & " km/h"

        ElseIf speed > 130 Then

            Label1.Text = "Speed: " & ((speed - 131) / -8.666).ToString("00") & " km/h"

        End If

        Label2.Text = "Steer: " & ((80 / 65535) * joystickState.X).ToString("00") - 40 & "°"


        If armDir = "l" Then
            Label3.Text = "Arm: LEFT"
        ElseIf armDir = "r" Then
            Label3.Text = "Arm: RIGHT"
        ElseIf armDir = "u" Then
            Label3.Text = "Arm: UP"
        ElseIf armDir = "d" Then
            Label3.Text = "Arm: DOWN"
        ElseIf armDir = "f" Then
            Label3.Text = "Arm: FORWARD"
        ElseIf armDir = "b" Then
            Label3.Text = "Arm: BACKWORDS"
        ElseIf armDir = "0" Then
            Label3.Text = "Arm: STOP"
        End If

        If em Then

            Label3.Font = New Font(Label1.Font, FontStyle.Bold)

        Else

            Label3.Font = New Font(Label1.Font, FontStyle.Regular)

        End If

        If led Then

            Label4.Text = "Lights: ON"

        Else

            Label4.Text = "Lights: OFF"

        End If






        'Dim msg As String = "b" & cameraX.ToString("000") & cameraY.ToString("000") & steer.ToString("000") & speed.ToString("000") & intLed.ToString & armDir & intEm & "e"

        Dim msg As String = "b" & steer.ToString("000") & speed.ToString("000") & intLed.ToString & armDir & intEm & "e"


        Dim data() As Byte = Encoding.ASCII.GetBytes(msg) 'Convert string to bytes
        udpClient.Send(data, data.Length)


    End Sub



    Sub stopRover()

        Timer1.Enabled = False

        em = False
        led = False
        prevJ = False
        prevJ1 = False

        Label1.Text = "Speed: 00 km/h"
        Label2.Text = "Steer: 00°"
        Label3.Text = "Arm: STOP"
        Label3.Font = New Font(Label1.Font, FontStyle.Regular)
        Label4.Text = "Lights: OFF"
        Button1.Text = "Start"
        If Music Then
            My.Computer.Audio.Stop()
        End If


        Timer3.Enabled = False
        t = time

        Label5.Text = ""

        Dim msg As String = "b000000000e"

        Dim data() As Byte = Encoding.ASCII.GetBytes(msg) 'Convert string to bytes
        udpClient.Send(data, data.Length)

    End Sub


    Sub startRover()

        Timer1.Enabled = True
        Label5.ForeColor = Color.Black
        Timer3.Enabled = True
        Button1.Text = "Stop"

        If Music Then
            My.Computer.Audio.Play(My.Resources.aa, AudioPlayMode.BackgroundLoop)
        End If



    End Sub


    Public Sub UDPRecv(ar As IAsyncResult)



        Try
            '' Next statement will throw when the socket was closed
            Dim recvBytes As Byte() = receivingUdpClient.EndReceive(ar, RemoteIPEndPoint)
            Dim recvMsg As String = Encoding.UTF8.GetString(recvBytes)
            '' Pass the string to a method that runs on the UI thread
            Me.BeginInvoke(New Action(Of String)(AddressOf DataReceived), recvMsg)
            '' Continue receiving
            receivingUdpClient.BeginReceive(AddressOf UDPRecv, Nothing)
        Catch ex As ObjectDisposedException
            '' Socket was closed, do nothing
        End Try



    End Sub

    Private Sub DataReceived(recvMsg As String)

        If recvMsg = "start" Then

            startRover()

        ElseIf recvMsg = "stop" Then

            stopRover()

        End If

    End Sub


    Private Sub Form1_FormClosing(sender As Object, e As FormClosingEventArgs) Handles MyBase.FormClosing

        Try

            receivingUdpClient.Close()
        Catch
        End Try

        Try

            udpClient.Close()
        Catch
        End Try

        Form2.Close()

    End Sub

    Private Sub Timer3_Tick(sender As Object, e As EventArgs) Handles Timer3.Tick

        t = t - 1

        Dim iSpan As TimeSpan = TimeSpan.FromSeconds(t)

        Label5.Text = "TIME: " & iSpan.Minutes.ToString.PadLeft(2, "0"c) & ":" & iSpan.Seconds.ToString.PadLeft(2, "0"c)

        If t = 10 Then

            Label5.ForeColor = Color.Red

        End If

        If t = 0 Then

            stopRover()

        End If

    End Sub
End Class