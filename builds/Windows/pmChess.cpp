/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

#include <windows.h>
// wcout():
#include <iostream>
// wcsnlen_s(), wcscmp(), size_t:
#include <wchar.h>
// PeekMessageW():
#include <winuser.h>

#define MAX_CMD_LENGTH (static_cast<size_t>(64))

int WINAPI wWinMain(
	_In_ HINSTANCE, // not used
	_In_opt_ HINSTANCE, // not used
	_In_ PWSTR pCmdLine,
	_In_ int) // not used
{
	auto return_code = static_cast<int>(0);
	
	HANDLE output_reader = NULL;
	HANDLE output_writer = NULL;
	
	STARTUPINFO startup_information;
	PROCESS_INFORMATION process_information;
	SECURITY_ATTRIBUTES security_attributes;
	
	SecureZeroMemory(&startup_information, sizeof(startup_information));
	SecureZeroMemory(&process_information, sizeof(process_information));
	SecureZeroMemory(&security_attributes, sizeof(security_attributes));
	
	startup_information.cb = sizeof(startup_information);
	
	__try
	{	
		wchar_t application_name[MAX_PATH];
		wchar_t command_line[MAX_PATH + MAX_CMD_LENGTH];
		wchar_t current_directory[MAX_PATH];
		auto application_name_length = GetModuleFileNameW(NULL, application_name, MAX_PATH);
		auto command_line_length = static_cast<DWORD>(0);
		auto current_directory_length = static_cast<DWORD>(0);
		
		{ // Grap focus (such that pmChess window will have focus and is not in background opened):
			MSG message = { 0 };
			PeekMessageW(&message, NULL, static_cast<UINT>(0), static_cast<UINT>(0), PM_NOREMOVE);
		}
		
		if (!application_name_length
			|| application_name_length > MAX_PATH - static_cast<DWORD>(70))
		{
			std::wcout
				<< std::endl
				<< L"Failed to start pmChess (to long executable path)."
				<< std::endl;
			return static_cast<int>(2);
		}
		for (auto i = static_cast<DWORD>(0); i < application_name_length; i++)
		{
			command_line[i] = application_name[i];
			current_directory[i] = application_name[i];
			if (application_name[i] == L'\\')
			{
				current_directory_length = i;
			}
		}
		application_name_length = command_line_length = current_directory_length;
		current_directory[current_directory_length++] = L'\0';
		for (const auto c : L"\\binaries\\bin\\java.exe")
		{
			application_name[application_name_length++] = c;
			command_line[command_line_length++] = c;
		}
		command_line_length--; // Delete termination L'\0'.
		for (const auto c : L" -m pmchess/pmchess.pmChess")
		{
			command_line[command_line_length++] = c;
		}
		
		const auto pCmdLine_length = wcsnlen_s(pCmdLine, MAX_CMD_LENGTH);
		if (pCmdLine_length == MAX_CMD_LENGTH)
		{
			std::wcout
				<< std::endl
				<< L"Failed to start pmChess (to long command line)."
				<< std::endl;
			return static_cast<int>(2);
		}
		else if (pCmdLine_length > static_cast<size_t>(0))
		{
			if (0 != wcscmp(pCmdLine, L"--debug"))
			{
				command_line_length--; // Delete termination L'\0'.
				command_line[command_line_length++] = L' ';
				for (auto i = static_cast<size_t>(0); i < pCmdLine_length; i++)
				{
					command_line[command_line_length++] = pCmdLine[i];
				}
				command_line[command_line_length++] = L'\0';
			}
			
			if (!AttachConsole(ATTACH_PARENT_PROCESS) && !AllocConsole())
			{
				std::wcout
					<< std::endl
					<< L"Failed to start pmChess "
						L"(missing output console for command line call)."
					<< std::endl;
				return_code = static_cast<int>(2);
			}
			else if ((security_attributes.nLength = sizeof(SECURITY_ATTRIBUTES)
					, security_attributes.bInheritHandle = TRUE
					, security_attributes.lpSecurityDescriptor = NULL
					, !CreatePipe(
						  &output_reader
						, &output_writer
						, &security_attributes
						, static_cast<DWORD>(0)))
				|| !SetHandleInformation(output_reader, HANDLE_FLAG_INHERIT, static_cast<DWORD>(0)))
			{
				std::wcout
					<< std::endl
					<< L"Failed to start pmChess "
						L"(connection failure with command line call console)."
					<< std::endl;
				return_code = static_cast<int>(2);
			}
			else
			{
				startup_information.hStdOutput = output_writer;
				startup_information.hStdError = output_writer;
				startup_information.dwFlags |= STARTF_USESTDHANDLES;
			}
		}
		
		if (static_cast<int>(0) != return_code)
		{
		}
		else if (!CreateProcessW(
			  application_name
			, command_line // Must be writeable (e.g., NOT 'const wchar_t*' or literal).
			, NULL // Process handle of created process is not inheritable.
			, NULL // Thread handle of created process is not inheritable.
			, pCmdLine_length > static_cast<size_t>(0) // Inherite handles of current process iff cmd call.
			, CREATE_NO_WINDOW | INHERIT_PARENT_AFFINITY | CREATE_UNICODE_ENVIRONMENT
			, NULL // Use environment of current process.
			, current_directory
			, &startup_information
			, &process_information))
		{
			std::wcout
				<< std::endl
				<< L"Failed to start pmChess (process creation error code: %d)."
				<< GetLastError()
				<< std::endl;
			return_code = static_cast<int>(2);
		}
		else
		{
			if (pCmdLine_length > static_cast<size_t>(0))
			{
				CloseHandle(output_writer);
				
				DWORD read, written;
				CHAR /* Always byte-wise write/read. */ buffer[static_cast<size_t>(4096)];
				HANDLE parent_stdout = GetStdHandle(STD_OUTPUT_HANDLE);
				
				while (ReadFile(output_reader, buffer, sizeof(buffer), &read, NULL)
					&& static_cast<DWORD>(0) != read
					&& WriteFile(parent_stdout, buffer, read, &written, NULL))
				{
				}
			}
			
			WaitForSingleObject(process_information.hProcess, INFINITE);
			
			DWORD exit_code;
			if (GetExitCodeProcess(process_information.hProcess, &exit_code))
			{
				return_code = static_cast<int>(exit_code);
			}
			else
			{
				std::wcout
					<< std::endl
					<< L"Error while terminating pmChess (failed to retrieve exit code)."
					<< std::endl;
				return_code = static_cast<int>(2);
			}
		}
	}
	__except(EXCEPTION_EXECUTE_HANDLER)
	{
		std::wcout
			<< std::endl
			<< L"Error while executing pmChess (runtime exception error code: %i)."
			<< GetExceptionCode()
			<< std::endl;
		return_code = static_cast<int>(2);
	}
	
	CloseHandle(process_information.hProcess);
	CloseHandle(process_information.hThread);
	CloseHandle(output_reader);
	CloseHandle(output_writer);
	
	SecureZeroMemory(&startup_information, sizeof(startup_information));
	SecureZeroMemory(&process_information, sizeof(process_information));
	SecureZeroMemory(&security_attributes, sizeof(security_attributes));
	
	return return_code;
}

#undef MAX_CMD_LENGTH
