package ru.ardu_cris.mai;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Модуль приложения Matreshka Analytics Integration
 * @author aleksandr<kopilov.ad@mail.com>
 */
public interface ApplicationModule {
	/**
	 * Возвращает опции командной строки, с которыми надо запускать модуль
	 * @return 
	 */
	public Options getOptionsList();
	
	/**
	 * Проверка парамтеров командной строки на корректность
	 * @param commandLine распознанная командная строка
	 * @return текст ошибки для пользователя, null, если параметры валидны.
	 */
	public String validateParameters(CommandLine commandLine);
	/**
	 * Запуск модуля с распознанными параметрами командной строки
	 * @param commandLine распознанная командная строка
	 * @return код возврата для ОС
	 */
	public int execute(CommandLine commandLine);
	
	public String getHelpMessage();
}
