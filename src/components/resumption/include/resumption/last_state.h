/**
 * Copyright (c) 2013, Ford Motor Company
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the Ford Motor Company nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef SRC_COMPONENTS_RESUMPTION_INCLUDE_RESUMPTION_LAST_STATE_H_
#define SRC_COMPONENTS_RESUMPTION_INCLUDE_RESUMPTION_LAST_STATE_H_

#include <string>

#include "utils/macro.h"
#include "utils/dict.h"
#include "utils/singleton.h"
#include "json/json.h"

namespace resumption {

class LastState : public utils::Singleton<LastState> {
 public:
/**
 * @brief Typedef for string-driven dictionary
 */
  typedef utils::Dictionary<std::string, std::string> Dictionary;
/**
 * @brief public dictionary
 */
  Dictionary dictionary;

  /**
    * @brief Convert utils::Dictionary<std::string, std::string> to Json
    * @param dict - input dictionary
    * @return created Json value
    */
  static Json::Value toJson(const Dictionary& dict);

  /**
    * @brief Convert Json to utils::Dictionary<std::string, std::string>
    * @param json_val - Json Kson
    * @return created Dictionary
    */
  static Dictionary fromJson(const Json::Value& json_val);

  private:

  /**
   * @brief File to save Dictionary
   */
  static const std::string filename;

  /**
   * @brief Saving dictionary to filesystem as Json
   */
  void SaveToFileSystem();

  /**
   * @brief Load dictionary from filesystem as Json
   */
  void LoadFromFileSystem();

 /**
 * @brief Private default constructor
 */
  LastState();
  ~LastState();


  DISALLOW_COPY_AND_ASSIGN(LastState);

  FRIEND_BASE_SINGLETON_CLASS(LastState);
};

}  // namespace resumption

#endif  // SRC_COMPONENTS_RESUMPTION_INCLUDE_RESUMPTION_LAST_STATE_H_
