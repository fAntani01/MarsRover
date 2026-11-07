Public Class Form2


    Private Sub Button1_Click(sender As Object, e As EventArgs) Handles Button1.Click

        Form1.RoverIP = TextBox1.Text
        Form1.RoverPort = TextBox2.Text
        Form1.AndroidPort = TextBox3.Text
        Form1.StreamingPort = TextBox4.Text
        Form1.Music = CheckBox1.Checked

        If RadioButton1.Checked Then

            Form1.Streaming = 1

        ElseIf RadioButton2.Checked Then

            Form1.Streaming = 2

        ElseIf Not (RadioButton1.Checked And RadioButton2.Checked) Then

            Form1.Streaming = 0

        End If

        Form1.time = TextBox5.Text

        Form1.Show()
        Me.Hide()

    End Sub

    Private Sub TextBox1_TextChanged(sender As Object, e As EventArgs) Handles TextBox1.TextChanged

        If TextBox1.Text <> "" And TextBox2.Text <> "" And TextBox3.Text <> "" And TextBox4.Text <> "" Then

            Button1.Enabled = True

        Else

            Button1.Enabled = False

        End If

    End Sub

    Private Sub TextBox2_TextChanged(sender As Object, e As EventArgs) Handles TextBox2.TextChanged

        If TextBox1.Text <> "" And TextBox2.Text <> "" And TextBox3.Text <> "" And TextBox4.Text <> "" Then

            Button1.Enabled = True

        Else

            Button1.Enabled = False

        End If

    End Sub

    Private Sub TextBox3_TextChanged(sender As Object, e As EventArgs) Handles TextBox3.TextChanged

        If TextBox1.Text <> "" And TextBox2.Text <> "" And TextBox3.Text <> "" And TextBox4.Text <> "" Then

            Button1.Enabled = True

        Else

            Button1.Enabled = False

        End If

    End Sub

    Private Sub TextBox4_TextChanged(sender As Object, e As EventArgs) Handles TextBox4.TextChanged

        If TextBox1.Text <> "" And TextBox2.Text <> "" And TextBox3.Text <> "" And TextBox4.Text <> "" Then

            Button1.Enabled = True

        Else

            Button1.Enabled = False

        End If

    End Sub
End Class