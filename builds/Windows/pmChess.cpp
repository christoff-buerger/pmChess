/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

#include <windows.h>
#include <iostream>

int WINAPI wWinMain(
	_In_ HINSTANCE, // not used
	_In_opt_ HINSTANCE, // not used
	_In_ LPWSTR, // not used
	_In_ int) // not used
{
	auto return_code = 0;
	STARTUPINFO startup_information;
	PROCESS_INFORMATION process_information;
	SecureZeroMemory(&startup_information, sizeof(startup_information));
	SecureZeroMemory(&process_information, sizeof(process_information));
	
	__try
	{
		startup_information.cb = sizeof(startup_information);
		
		wchar_t application_name[MAX_PATH];
		wchar_t command_line[MAX_PATH];
		wchar_t current_directory[MAX_PATH];
		auto application_name_length = GetModuleFileNameW(0, application_name, MAX_PATH);
		auto command_line_length = 0ul;
		auto current_directory_length = 0ul;
		if (!application_name_length || application_name_length > MAX_PATH - 70)
		{
			std::cout
				<< std::endl
				<< L"Failed to start pmChess (to long executable path)."
				<< std::endl;
			return 2;
		}
		for (auto i = 0ul; i < application_name_length; i++)
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
		command_line_length--;
		for (const auto c : L" -m pmchess/pmchess.pmChess")
		{
			command_line[command_line_length++] = c;
		}
		
		if (!CreateProcessW(
			application_name,
			command_line, // Must be writeable and not read-only like const wchar_t* or literal.
			NULL, // Process handle of created process is not inheritable.
			NULL, // Thread handle of created process is not inheritable.
			FALSE, // Do not inherite handles of current process.
			CREATE_NO_WINDOW | CREATE_UNICODE_ENVIRONMENT | INHERIT_PARENT_AFFINITY,
			NULL, // Use environment of current process.
			current_directory,
			&startup_information,
			&process_information))
		{
			std::cout
				<< std::endl
				<< L"Failed to start pmChess (process creation error code: %d)."
				<< GetLastError()
				<< std::endl;
			return 2;
		}
		
		WaitForSingleObject(process_information.hProcess, INFINITE);
	}
	__except(EXCEPTION_EXECUTE_HANDLER)
	{
		std::cout
			<< std::endl
			<< L"Internal pmChess error (runtime exception error code: %i)."
			<< GetExceptionCode()
			<< std::endl;
		return_code = 2;
	}
	
	CloseHandle(process_information.hProcess);
	CloseHandle(process_information.hThread);
	SecureZeroMemory(&startup_information, sizeof(startup_information));
	SecureZeroMemory(&process_information, sizeof(process_information));
	
	return return_code;
}
